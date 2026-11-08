package backend.globber.common.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

@Builder
public record PresignedUrlResponse(
    String presignedUrl,
    String s3Key,

    @Schema(
        description = "Presigned URL로 PUT할 때 그대로 보내야 하는 헤더(헤더 이름을 키로 하는 객체). 서명에 포함되므로 값이 다르면 업로드가 거절됩니다.",
        example = "{\"Content-Type\": \"image/jpeg\", \"Cache-Control\": \"public, max-age=31536000, immutable\"}"
    )
    Map<String, String> uploadHeaders
) {

    public static PresignedUrlResponse of(String presignedUrl, String s3Key, Map<String, String> uploadHeaders) {
        return PresignedUrlResponse.builder()
            .presignedUrl(presignedUrl)
            .s3Key(s3Key)
            .uploadHeaders(uploadHeaders)
            .build();
    }
}
