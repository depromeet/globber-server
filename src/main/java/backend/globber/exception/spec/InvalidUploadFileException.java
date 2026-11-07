package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidUploadFileException extends CustomException {

    public InvalidUploadFileException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
