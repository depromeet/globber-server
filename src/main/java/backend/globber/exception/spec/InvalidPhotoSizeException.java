package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidPhotoSizeException extends CustomException {

    public InvalidPhotoSizeException() {
        super(HttpStatus.BAD_REQUEST, "사진의 너비와 높이는 1 이상이어야 합니다.");
    }
}
