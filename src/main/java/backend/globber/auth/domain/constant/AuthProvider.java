package backend.globber.auth.domain.constant;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum AuthProvider {
    KAKAO(1);

    private final int code;

    AuthProvider(int code) {
        this.code = code;
    }

    public static AuthProvider of(int code) {
        return Arrays.stream(AuthProvider.values())
            .filter(ap -> ap.getCode() == code)
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 로그인 서비스입니다."));
    }
}
