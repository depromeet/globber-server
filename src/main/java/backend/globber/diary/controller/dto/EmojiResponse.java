package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.DiaryEmoji;

public record EmojiResponse(
        String code,
        String glyph,
        Long count
) {
    public static EmojiResponse from(DiaryEmoji emoji) {
        return new EmojiResponse(
                emoji.getCode(),
                emoji.getGlyph(),
                emoji.getCount()
        );
    }
}
