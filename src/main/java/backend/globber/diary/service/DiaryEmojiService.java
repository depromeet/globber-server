package backend.globber.diary.service;

import backend.globber.diary.controller.dto.EmojiResponse;

import java.util.List;

public interface DiaryEmojiService {
    EmojiResponse registerEmoji(Long diaryId, String code, String glyph);

    void pressEmoji(Long diaryId, String code);

    List<EmojiResponse> getEmojis(Long diaryId);
}
