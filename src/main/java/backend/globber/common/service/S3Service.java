package backend.globber.common.service;

import backend.globber.common.controller.dto.request.PresignedUrlRequest;
import backend.globber.common.controller.dto.response.PresignedUrlResponse;
import backend.globber.common.enums.S3UploadType;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes:15}")
    private int expirationMinutes;

    public String generateUniqueKey(String prefix, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        return String.format("%s/%s-%s%s", prefix, timestamp, uuid, extension);
    }

    public PresignedUrlResponse generatePresignedUrl(Long memberId, PresignedUrlRequest request) {
        final S3UploadType uploadType = request.uploadType();
        final Long resourceId = request.resourceId();
        final String fileName = request.fileName();
        final String contentType = request.contentType();

        String prefix = uploadType.generatePrefix(memberId, resourceId);

        String s3Key = generateUniqueKey(prefix, fileName);

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .putObjectRequest(builder -> builder
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build())
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        log.info("Presigned URL 생성 완료 - memberId: {}, uploadType: {}, s3Key: {}", memberId, uploadType, s3Key);

        return PresignedUrlResponse.of(presignedUrl, s3Key);
    }
}
