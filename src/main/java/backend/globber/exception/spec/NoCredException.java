package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NoCredException extends CustomException {
    public NoCredException() {
        super(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
    }
}
