package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DiaryNotFoundException extends CustomException {

    public DiaryNotFoundException() {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 기록입니다.");
    }

    public DiaryNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
