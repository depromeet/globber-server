package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CustomIOException extends CustomException {

    public CustomIOException() {
        super(HttpStatus.BAD_GATEWAY, "IO Exception");
    }

    public CustomIOException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}