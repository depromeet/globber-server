package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UsernameNotFoundException extends CustomException {

    public UsernameNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다.");
    }

    public UsernameNotFoundException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
