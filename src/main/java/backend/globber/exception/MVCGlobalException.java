package backend.globber.exception;

import backend.globber.auth.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
class MVCGlobalException {

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
}
