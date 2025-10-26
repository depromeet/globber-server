package backend.globber.common.controller.dto.request;

import backend.globber.common.enums.S3UploadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PresignedUrlRequest(
    @NotNull(message = "업로드 타입은 필수입니다.")
    @Schema(description = "업로드 타입 (PROFILE: 프로필 이미지, TRAVEL: 여행 이미지)", example = "PROFILE")
    S3UploadType uploadType,

    @Schema(description = "리소스 ID (TRAVEL 타입인 경우 필수)", example = "123")
    Long resourceId,

    @NotBlank(message = "파일명은 필수입니다.")
    @Schema(description = "업로드할 파일명 (확장자 포함)", example = "profile.jpg")
    String fileName,

    @NotBlank(message = "컨텐츠 타입은 필수입니다.")
    @Schema(description = "파일의 Content-Type", example = "image/jpeg")
    @Pattern(regexp = "^(image|video|application)/.+$", message = "올바른 Content-Type 형식이 아닙니다.")
    String contentType
) {

}
