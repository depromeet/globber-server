package backend.globber.common.service;

import backend.globber.common.constant.ImageUploadPolicy;
import backend.globber.exception.spec.InvalidUploadFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 업로드된 이미지를 기록·프로필에 연결하기 전에 S3 HEAD로 형식과 크기를 검사한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadedImageValidator {

    private static final String NOT_FOUND_MESSAGE = "업로드된 이미지를 찾을 수 없습니다.";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void validate(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new InvalidUploadFileException(NOT_FOUND_MESSAGE);
        }

        HeadObjectResponse head = headObject(s3Key);

        Long contentLength = head.contentLength();
        if (contentLength == null || contentLength > ImageUploadPolicy.MAX_SIZE_BYTES) {
            log.warn("업로드 이미지 크기 검증 실패 - s3Key: {}, contentLength: {}", s3Key, contentLength);
            throw new InvalidUploadFileException("이미지 크기는 10MB 이하여야 합니다.");
        }

        if (!ImageUploadPolicy.isAllowedStoredContentType(head.contentType())) {
            log.warn("업로드 이미지 형식 검증 실패 - s3Key: {}, contentType: {}", s3Key, head.contentType());
            throw new InvalidUploadFileException("지원하지 않는 이미지 형식입니다.");
        }
    }

    private HeadObjectResponse headObject(String s3Key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build();
        try {
            return s3Client.headObject(request);
        } catch (NoSuchKeyException e) {
            throw notFound(s3Key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw notFound(s3Key);
            }
            if (e.statusCode() == 403) {
                // ListBucket 권한이 없으면 S3는 객체가 없을 때도 403을 반환하므로 사용자에게는 400으로 응답한다.
                // 다만 IAM·버킷 정책 오류 같은 권한 장애도 같은 403이라, 구분할 수 있도록 ERROR로 남긴다.
                log.error("S3 HEAD 403 - 객체가 없거나 권한 문제일 수 있음 - s3Key: {}, requestId: {}",
                    s3Key, e.requestId());
                throw new InvalidUploadFileException(NOT_FOUND_MESSAGE);
            }
            throw e;
        }
    }

    private InvalidUploadFileException notFound(String s3Key) {
        log.warn("업로드 이미지를 찾을 수 없음 - s3Key: {}", s3Key);
        return new InvalidUploadFileException(NOT_FOUND_MESSAGE);
    }
}
