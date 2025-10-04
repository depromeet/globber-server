package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MailException extends CustomException {

    public MailException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "메일 전송에 실패했습니다.");
    }
}
