package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidSearchKeywordException extends CustomException {

    public InvalidSearchKeywordException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
