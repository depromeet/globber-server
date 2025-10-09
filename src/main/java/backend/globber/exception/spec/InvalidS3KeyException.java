package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidS3KeyException extends CustomException {

    public InvalidS3KeyException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
