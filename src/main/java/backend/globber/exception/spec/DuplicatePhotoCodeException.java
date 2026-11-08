package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DuplicatePhotoCodeException extends CustomException {

    public DuplicatePhotoCodeException() {
        super(HttpStatus.CONFLICT, "이미 다른 기록에서 사용 중인 사진입니다.");
    }
}
