package backend.globber.common.controller.dto.request;

import backend.globber.common.enums.S3UploadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlRequest(
    @NotNull(message = "업로드 타입은 필수입니다.")
    @Schema(description = "업로드 타입 (PROFILE: 프로필 이미지, TRAVEL: 여행 이미지)", example = "PROFILE")
    S3UploadType uploadType,

    @NotBlank(message = "파일명은 필수입니다.")
    @Schema(description = "업로드할 파일명 (확장자 포함)", example = "profile.jpg")
    String fileName,

    @NotBlank(message = "컨텐츠 타입은 필수입니다.")
    @Schema(description = "파일의 Content-Type (image/jpeg, image/png, image/webp, image/avif만 허용)", example = "image/jpeg")
    String contentType
) {

}
