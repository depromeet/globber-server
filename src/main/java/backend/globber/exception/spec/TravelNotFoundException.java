package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TravelNotFoundException extends CustomException{

    public TravelNotFoundException () {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 여행입니다.");
    }

    public TravelNotFoundException (String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
