package backend.globber.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.common.controller.dto.request.PresignedUrlRequest;
import backend.globber.common.controller.dto.response.PresignedUrlResponse;
import backend.globber.common.enums.S3UploadType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 실제 S3Presigner(가짜 자격증명, 네트워크 호출 없음)로 서명을 만들어
 * 클라이언트가 PUT할 때 보내야 하는 헤더가 실제로 서명에 포함되는지 확인한다.
 */
class S3ServiceSignedHeadersTest {

    private S3Presigner presigner;
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("AKIATESTTESTTESTTEST", "test-secret-key")))
            .build();
        s3Service = new S3Service(presigner);
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "expirationMinutes", 15);
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    @DisplayName("Content-Type과 Cache-Control이 서명 헤더에 포함되고 uploadHeaders와 값이 같다")
    void cacheControlIsSigned() {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest(S3UploadType.PROFILE, "image.jpg", "image/jpeg");

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(1L, request);

        // then
        String url = response.presignedUrl();
        assertThat(url).containsPattern("X-Amz-SignedHeaders=[^&]*cache-control");
        assertThat(url).containsPattern("X-Amz-SignedHeaders=[^&]*content-type");
        Map<String, String> uploadHeaders = response.uploadHeaders();
        assertThat(uploadHeaders)
            .containsEntry("Content-Type", "image/jpeg")
            .containsEntry("Cache-Control", "public, max-age=31536000, immutable");
    }
}
