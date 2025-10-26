package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BookmarkException extends CustomException {

    public BookmarkException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BookmarkException(HttpStatus status, String message) {
        super(status, message);
    }
}
