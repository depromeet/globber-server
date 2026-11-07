package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidEmojiCodeException extends CustomException {

    public InvalidEmojiCodeException() {
        super(HttpStatus.BAD_REQUEST, "올바르지 않은 이모지 코드입니다.");
    }
}
