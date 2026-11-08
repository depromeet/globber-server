package backend.globber.diary.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EmojiRequest(
        @Schema(description = "소문자 16진 유니코드 코드포인트. 여러 개면 -로 연결합니다. glyph는 서버가 만듭니다.", example = "1f607")
        @NotBlank(message = "이모지 코드는 필수입니다.")
        String code
) {
}
