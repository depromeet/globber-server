package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.DiaryEmoji;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmojiResponse(
        String code,
        String glyph,
        Long count,
        @Schema(description = "조회한 사람이 이 이모지를 눌렀는지. 비로그인이면 false")
        boolean pressedByMe
) {
    public static EmojiResponse from(DiaryEmoji emoji, boolean pressedByMe) {
        return new EmojiResponse(
                emoji.getCode(),
                emoji.getGlyph(),
                emoji.getCount(),
                pressedByMe
        );
    }
}
