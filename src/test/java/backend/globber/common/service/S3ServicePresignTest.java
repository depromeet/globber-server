package backend.globber.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.globber.common.controller.dto.request.PresignedUrlRequest;
import backend.globber.common.controller.dto.response.PresignedUrlResponse;
import backend.globber.common.enums.S3UploadType;
import backend.globber.exception.spec.InvalidUploadFileException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3ServicePresignTest {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "expirationMinutes", 15);
    }

    private PresignedUrlRequest request(String contentType) {
        return new PresignedUrlRequest(S3UploadType.PROFILE, "image.jpg", contentType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/avif"})
    @DisplayName("허용된 형식이면 Cache-Control을 서명에 포함하고 uploadHeaders를 내려준다")
    void generatePresignedUrlWithAllowedType(String contentType) throws Exception {
        // given
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/profiles/1/image.jpg"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(1L, request(contentType));

        // then
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        PutObjectRequest putObjectRequest = captor.getValue().putObjectRequest();
        assertThat(putObjectRequest.contentType()).isEqualTo(contentType);
        assertThat(putObjectRequest.cacheControl()).isEqualTo(CACHE_CONTROL);
        assertThat(putObjectRequest.key()).isEqualTo(response.s3Key());

        assertThat(response.presignedUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/profiles/1/image.jpg");
        assertThat(response.s3Key()).startsWith("profiles/1/");
        assertThat(response.uploadHeaders())
            .containsOnlyKeys("Content-Type", "Cache-Control")
            .containsEntry("Content-Type", contentType)
            .containsEntry("Cache-Control", CACHE_CONTROL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/heic", "video/mp4", "application/pdf", "image/jpeg; charset=utf-8", "IMAGE/JPEG"})
    @DisplayName("허용되지 않은 형식이면 예외가 발생하고 presign하지 않는다")
    void generatePresignedUrlWithRejectedType(String contentType) {
        // when & then
        assertThatThrownBy(() -> s3Service.generatePresignedUrl(1L, request(contentType)))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessageStartingWith("지원하지 않는 이미지 형식입니다.");
        verifyNoInteractions(s3Presigner);
    }
}
