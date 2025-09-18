package backend.globber.exception;

import backend.globber.auth.dto.response.ApiResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ApiResponse<String> response;

    public CustomException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.response = ApiResponse.fail(message);
    }

    public CustomException(HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.response = ApiResponse.fail(message);
    }
}
