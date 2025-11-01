package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DuplicateEmojiException extends CustomException {
    public DuplicateEmojiException() {
        super(HttpStatus.CONFLICT, "이미 존재하는 이모지입니다.");
    }
}
