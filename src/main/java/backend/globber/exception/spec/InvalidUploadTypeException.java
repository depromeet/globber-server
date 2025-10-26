package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidUploadTypeException extends CustomException {

    public InvalidUploadTypeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
