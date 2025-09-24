package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UUIDNotFoundException extends CustomException {

    public UUIDNotFoundException() {
        super(HttpStatus.NOT_FOUND, "사용자의 UUID가 존재하지 않습니다.");
    }

    public UUIDNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
