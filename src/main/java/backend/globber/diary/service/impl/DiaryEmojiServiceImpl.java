package backend.globber.diary.service.impl;

import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.domain.DiaryEmoji;
import backend.globber.diary.domain.EmojiCode;
import backend.globber.diary.repository.DiaryEmojiReactionRepository;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.service.DiaryEmojiService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.InvalidEmojiException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryEmojiServiceImpl implements DiaryEmojiService {

    private final DiaryRepository diaryRepository;
    private final DiaryEmojiRepository emojiRepository;
    private final DiaryEmojiReactionRepository reactionRepository;

    @Override
    @Transactional
    public EmojiResponse registerEmoji(Long memberId, Long diaryId, String code) {
        String glyph = EmojiCode.toGlyph(code);
        if (!diaryRepository.existsById(diaryId)) {
            throw new DiaryNotFoundException();
        }

        emojiRepository.insertIfAbsent(diaryId, code, glyph);
        // 반응 기록이 새로 들어갔을 때만 count를 올린다 (같은 사람이 동시에 여러 번 보내도 1번)
        if (reactionRepository.insertIfAbsent(diaryId, memberId, code) == 1) {
            emojiRepository.incrementCount(diaryId, code);
        }
        return response(memberId, diaryId, code);
    }

    @Override
    @Transactional
    public EmojiResponse pressEmoji(Long memberId, Long diaryId, String code) {
        EmojiCode.toGlyph(code);
        if (emojiRepository.findByDiaryIdAndCode(diaryId, code).isEmpty()) {
            throw new InvalidEmojiException();
        }

        if (reactionRepository.deleteReaction(diaryId, memberId, code) == 1) {
            emojiRepository.decrementCount(diaryId, code);
        } else if (reactionRepository.insertIfAbsent(diaryId, memberId, code) == 1) {
            emojiRepository.incrementCount(diaryId, code);
        }
        return response(memberId, diaryId, code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmojiResponse> getEmojis(Long viewerId, Long diaryId) {
        Set<String> pressed = viewerId == null
            ? Set.of()
            : new HashSet<>(reactionRepository.findCodes(diaryId, viewerId));
        return emojiRepository.findAllByDiaryIdOrderByCountDescCreatedAtAsc(diaryId)
            .stream()
            .map(emoji -> EmojiResponse.from(emoji, pressed.contains(emoji.getCode())))
            .toList();
    }

    private EmojiResponse response(Long memberId, Long diaryId, String code) {
        DiaryEmoji emoji = emojiRepository.findByDiaryIdAndCode(diaryId, code)
            .orElseThrow(InvalidEmojiException::new);
        boolean pressed = reactionRepository.findCodes(diaryId, memberId).contains(code);
        return EmojiResponse.from(emoji, pressed);
    }
}
