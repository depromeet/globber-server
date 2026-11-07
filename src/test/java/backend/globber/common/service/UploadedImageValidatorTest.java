package backend.globber.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.globber.exception.spec.InvalidUploadFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class UploadedImageValidatorTest {

    private static final String BUCKET = "test-bucket";
    private static final String S3_KEY = "profiles/1/image.jpg";
    private static final long MAX_SIZE = 10_485_760L;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private UploadedImageValidator validator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "bucketName", BUCKET);
    }

    private void givenHead(Long contentLength, String contentType) {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(
            HeadObjectResponse.builder()
                .contentLength(contentLength)
                .contentType(contentType)
                .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/avif", " IMAGE/JPEG ", "image/png; charset=binary"})
    @DisplayName("허용된 형식이고 10MB 이하이면 통과한다")
    void validateAllowedType(String contentType) {
        // given
        givenHead(1024L, contentType);

        // when & then
        assertThatCode(() -> validator.validate(S3_KEY)).doesNotThrowAnyException();

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo(S3_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/heic", "video/mp4", "application/pdf", "image/gif"})
    @DisplayName("허용되지 않은 형식이면 예외가 발생한다")
    void validateRejectedType(String contentType) {
        // given
        givenHead(1024L, contentType);

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("지원하지 않는 이미지 형식입니다.");
    }

    @Test
    @DisplayName("Content-Type이 없으면 예외가 발생한다")
    void validateNullContentType() {
        // given
        givenHead(1024L, null);

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("지원하지 않는 이미지 형식입니다.");
    }

    @Test
    @DisplayName("크기가 정확히 10MB이면 통과한다")
    void validateExactMaxSize() {
        // given
        givenHead(MAX_SIZE, "image/jpeg");

        // when & then
        assertThatCode(() -> validator.validate(S3_KEY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("크기가 10MB를 초과하면 예외가 발생한다")
    void validateOverMaxSize() {
        // given
        givenHead(MAX_SIZE + 1, "image/jpeg");

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("이미지 크기는 10MB 이하여야 합니다.");
    }

    @Test
    @DisplayName("Content-Length가 없으면 예외가 발생한다")
    void validateNullContentLength() {
        // given
        givenHead(null, "image/jpeg");

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("이미지 크기는 10MB 이하여야 합니다.");
    }

    @Test
    @DisplayName("객체가 없으면(NoSuchKeyException) 예외가 발생한다")
    void validateNoSuchKey() {
        // given
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(NoSuchKeyException.builder().message("not found").build());

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("업로드된 이미지를 찾을 수 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 403})
    @DisplayName("S3가 404 또는 403을 반환하면 객체 없음 예외가 발생한다")
    void validateNotFoundStatus(int statusCode) {
        // given
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(statusCode).build());

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("업로드된 이미지를 찾을 수 없습니다.");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("S3가 403을 반환하면 권한 장애일 수 있으므로 ERROR 로그를 남긴다")
    void validateForbiddenLogsError(CapturedOutput output) {
        // given
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(403).build());

        // when
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class);

        // then
        assertThat(output.getOut()).containsPattern("ERROR.*S3 HEAD 403.*" + S3_KEY);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("S3가 404를 반환하면 ERROR 로그를 남기지 않는다")
    void validateNotFoundDoesNotLogError(CapturedOutput output) {
        // given
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(404).build());

        // when
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isInstanceOf(InvalidUploadFileException.class);

        // then
        assertThat(output.getOut()).doesNotContainPattern("ERROR.*S3 HEAD");
    }

    @Test
    @DisplayName("그 밖의 S3 오류는 그대로 전파한다")
    void validateServerErrorPropagates() {
        // given
        S3Exception serverError = (S3Exception) S3Exception.builder().statusCode(500).build();
        given(s3Client.headObject(any(HeadObjectRequest.class))).willThrow(serverError);

        // when & then
        assertThatThrownBy(() -> validator.validate(S3_KEY))
            .isSameAs(serverError);
    }

    @Test
    @DisplayName("s3Key가 비어 있으면 S3를 호출하지 않고 예외가 발생한다")
    void validateBlankKey() {
        // when & then
        assertThatThrownBy(() -> validator.validate(" "))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("업로드된 이미지를 찾을 수 없습니다.");
        verifyNoInteractions(s3Client);
    }
}
