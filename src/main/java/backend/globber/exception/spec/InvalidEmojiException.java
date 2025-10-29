package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidEmojiException extends CustomException {
    public InvalidEmojiException() {
        super(HttpStatus.BAD_REQUEST, "등록되지 않은 이모지입니다.");
    }
}
