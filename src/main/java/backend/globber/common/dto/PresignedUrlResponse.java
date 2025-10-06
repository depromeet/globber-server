package backend.globber.common.dto;

import lombok.Builder;

@Builder
public record PresignedUrlResponse(
    String presignedUrl,
    String s3Key
) {

    public static PresignedUrlResponse of(String presignedUrl, String s3Key) {
        return PresignedUrlResponse.builder()
            .presignedUrl(presignedUrl)
            .s3Key(s3Key)
            .build();
    }
}
