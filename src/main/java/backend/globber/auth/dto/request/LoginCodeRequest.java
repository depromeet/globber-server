package backend.globber.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginCodeRequest(
        @Schema(description = "로그인 성공 리다이렉트로 받은 일회용 code (60초, 한 번만 사용)")
        @NotBlank(message = "code는 필수입니다.")
        String code
) {
}
