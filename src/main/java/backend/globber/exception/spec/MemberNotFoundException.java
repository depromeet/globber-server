package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends CustomException {

    public MemberNotFoundException() {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");
    }
}
