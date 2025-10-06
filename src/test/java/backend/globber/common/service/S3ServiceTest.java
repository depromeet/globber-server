package backend.globber.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.common.controller.dto.request.PresignedUrlRequest;
import backend.globber.common.dto.PresignedUrlResponse;
import backend.globber.common.enums.S3UploadType;
import backend.globber.config.S3Config;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(classes = {
    S3Service.class,
    S3Config.class  // S3 관련 설정 클래스만
})
class S3ServiceTest {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Test
    @DisplayName("실제 S3에 파일이 업로드되는지 테스트")
    void uploadToS3() throws Exception {
        // given
        Long memberId = 123L;
        PresignedUrlRequest request = new PresignedUrlRequest(
            S3UploadType.PROFILE,
            null,
            "test.jpg",
            "image/jpeg"
        );

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(memberId, request);

        // 실제 파일 업로드
        byte[] testImageData = createTestImageBytes();
        uploadToPresignedUrl(response.presignedUrl(), testImageData, "image/jpeg");

        // then - S3에 파일이 실제로 존재하는지 확인
        boolean exists = s3Client.headObject(builder -> builder
                .bucket(bucketName)
                .key(response.s3Key()))
            .sdkHttpResponse()
            .isSuccessful();

        assertThat(exists).isTrue();

        // 삭제
        s3Client.deleteObject(builder -> builder
            .bucket(bucketName)
            .key(response.s3Key()));
    }

    private byte[] createTestImageBytes() {
        // 간단한 1x1 픽셀 JPEG
        return new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
    }

    private void uploadToPresignedUrl(String presignedUrl, byte[] data, String contentType)
        throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(presignedUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", contentType);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(data);
        }

        int responseCode = connection.getResponseCode();
        assertThat(responseCode).isEqualTo(200);
    }
}
