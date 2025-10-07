package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PhotoCountException extends CustomException {

    public PhotoCountException(){
        super(HttpStatus.FORBIDDEN, "사진 개수가 일치하지 않습니다.");
    }
    public PhotoCountException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
