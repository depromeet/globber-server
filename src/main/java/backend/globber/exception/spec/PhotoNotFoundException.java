package backend.globber.exception.spec;

import backend.globber.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PhotoNotFoundException extends CustomException {

  public PhotoNotFoundException() {
    super(HttpStatus.NOT_FOUND, "존재하지 않는 사진입니다.");
  }

  public PhotoNotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }
}
