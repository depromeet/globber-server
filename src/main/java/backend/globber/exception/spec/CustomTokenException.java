package backend.globber.exception.spec;


import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CustomTokenException extends CustomException {

    public CustomTokenException() {
        super(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    public CustomTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public CustomTokenException(HttpStatus status, String message) {
        super(status, message);
    }

}
