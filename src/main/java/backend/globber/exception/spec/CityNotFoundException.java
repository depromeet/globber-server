package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CityNotFoundException extends CustomException {

    public CityNotFoundException () {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 도시입니다.");
    }

    public CityNotFoundException (String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
