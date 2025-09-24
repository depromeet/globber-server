package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ClientException extends CustomException {

    public ClientException() {
        super(HttpStatus.BAD_GATEWAY, "외부 서비스 요청 중 오류가 발생했습니다.");
    }

    public ClientException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
