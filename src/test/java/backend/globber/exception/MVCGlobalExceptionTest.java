package backend.globber.exception;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.common.dto.ApiResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;

class MVCGlobalExceptionTest {

    private final MVCGlobalException handler = new MVCGlobalException();

    @SuppressWarnings("unused")
    private void sample(String header) {
    }

    private MethodParameter parameter() throws NoSuchMethodException {
        Method method = MVCGlobalExceptionTest.class.getDeclaredMethod("sample", String.class);
        return new MethodParameter(method, 0);
    }

    @Test
    @DisplayName("Authorization이 아닌 헤더가 없으면 400을 반환한다")
    void missingOtherHeader_badRequest() throws Exception {
        // given
        MissingRequestHeaderException e = new MissingRequestHeaderException("X-Custom", parameter());

        // when
        ResponseEntity<ApiResponse<String>> response = handler.missingRequestHeaderHandler(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo("fail");
        assertThat(response.getBody().data()).contains("X-Custom");
    }

    @Test
    @DisplayName("Authorization 헤더 이름은 대소문자와 관계없이 401로 처리한다")
    void missingAuthorizationHeaderCaseInsensitive_unauthorized() throws Exception {
        // given
        MissingRequestHeaderException e = new MissingRequestHeaderException("authorization", parameter());

        // when
        ResponseEntity<ApiResponse<String>> response = handler.missingRequestHeaderHandler(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().data()).isEqualTo("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 415와 ApiResponse 형식을 반환한다")
    void unsupportedMediaType_415() {
        // given
        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException("not supported");

        // when
        ResponseEntity<ApiResponse<String>> response = handler.mediaTypeNotSupportedHandler(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().status()).isEqualTo("fail");
    }

    @Test
    @DisplayName("처리하지 못한 예외는 500과 고정 문구를 반환하고 원본 메시지를 노출하지 않는다")
    void unexpectedException_internalServerError() {
        // given
        Exception e = new IllegalStateException("backend.globber.secret.Internal failure");

        // when
        ResponseEntity<ApiResponse<String>> response = handler.unexpectedExceptionHandler(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo("fail");
        assertThat(response.getBody().data()).isEqualTo("서버 오류가 발생했습니다.");
    }
}
