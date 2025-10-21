package backend.globber.exception;

import org.springframework.http.HttpStatus;

public class BookmarkNotFoundException extends CustomException {

    public BookmarkNotFoundException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BookmarkNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
