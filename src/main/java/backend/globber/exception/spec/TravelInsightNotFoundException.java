package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TravelInsightNotFoundException extends CustomException {

    public TravelInsightNotFoundException() {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 인사이트입니다.");
    }

    public TravelInsightNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
