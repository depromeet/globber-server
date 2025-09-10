package backend.globber.exception.spec;


import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CustomTokenException extends CustomException {
    public CustomTokenException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 관련 예외가 발생했습니다.");
    }
    public CustomTokenException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
