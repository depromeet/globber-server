package backend.globber.profile.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateProfileImageRequest(
    @NotBlank(message = "S3 key는 필수입니다")
    String s3Key
) {

}
