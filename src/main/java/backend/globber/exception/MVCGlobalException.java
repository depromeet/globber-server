package backend.globber.exception;

import backend.globber.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
class MVCGlobalException {

    private static final String INVALID_VALUE_MESSAGE = "잘못된 요청 값입니다.";
    private static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요합니다.";
    private static final String INTERNAL_ERROR_MESSAGE = "서버 오류가 발생했습니다.";

    // CustomException을 처리하는 핸들러 -> RuntimeException을 상속받아서 사용
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> customExceptionHandler(CustomException e) {
        log.error("MVCGlobalException: " + e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(e.getResponse());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgumentHandler(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }

    // Authorization 헤더 누락은 인증이 필요한 상황이므로 401, 그 밖의 헤더 누락은 400
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<String>> missingRequestHeaderHandler(MissingRequestHeaderException e) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(e.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(LOGIN_REQUIRED_MESSAGE));
        }
        log.warn("필수 헤더 누락: {}", e.getHeaderName());
        return ResponseEntity.badRequest().body(ApiResponse.fail("필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> missingRequestParameterHandler(
        MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail("필수 요청 값이 누락되었습니다: " + e.getParameterName()));
    }

    // 타입 변환·본문 파싱 실패는 원본 예외 문구(내부 클래스명 등)를 노출하지 않고 고정 문구로 응답한다
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<String>> invalidValueHandler(Exception e) {
        log.warn("잘못된 요청 값: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(INVALID_VALUE_MESSAGE));
    }

    // @Valid 실패는 DTO에 작성된 검증 메시지를 그대로 쓴다
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> methodArgumentNotValidHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .findFirst()
            .orElse(INVALID_VALUE_MESSAGE);
        log.warn("요청 검증 실패: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<String>> handlerMethodValidationHandler(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .findFirst()
            .orElse(INVALID_VALUE_MESSAGE);
        log.warn("요청 검증 실패: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> methodNotSupportedHandler(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.fail("지원하지 않는 요청 방식입니다."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> mediaTypeNotSupportedHandler(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(ApiResponse.fail("지원하지 않는 Content-Type입니다."));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<String>> notFoundHandler(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("요청한 경로를 찾을 수 없습니다."));
    }

    // 위에서 처리하지 못한 예외는 원본 메시지를 응답에 싣지 않고 로그에만 남긴다
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> unexpectedExceptionHandler(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(INTERNAL_ERROR_MESSAGE));
    }
}
