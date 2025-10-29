package backend.globber.diary.service.impl;

import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.DiaryEmoji;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.service.DiaryEmojiService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.DuplicateEmojiException;
import backend.globber.exception.spec.InvalidEmojiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryEmojiServiceImpl implements DiaryEmojiService {

    private final DiaryRepository diaryRepository;
    private final DiaryEmojiRepository emojiRepository;

    @Override
    @Transactional
    public EmojiResponse registerEmoji(Long diaryId, String code, String glyph) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        try {
            DiaryEmoji emoji = DiaryEmoji.builder()
                    .diary(diary)
                    .code(code)
                    .glyph(glyph)
                    .count(0L)
                    .build();

            return EmojiResponse.from(emojiRepository.save(emoji));

        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmojiException();
        }
    }

    @Override
    @Transactional
    public void pressEmoji(Long diaryId, String code) {
        int updated = emojiRepository.incrementCount(diaryId, code);
        if (updated == 0) {
            throw new InvalidEmojiException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmojiResponse> getEmojis(Long diaryId) {
        return emojiRepository.findAllByDiaryIdOrderByCountDescCreatedAtAsc(diaryId)
                .stream().map(EmojiResponse::from).toList();
    }
}
