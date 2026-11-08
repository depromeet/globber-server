package backend.globber.auth.util;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieProvider {

    // jwt.refresh_expiration은 밀리초 단위. 쿠키 Max-Age는 초 단위라 변환해서 쓴다
    @Value("${jwt.refresh_expiration}")
    private String refreshExpire;

    // 운영은 Lax. 개발 서버는 로컬 프론트(localhost:3000)에서 재발급을 테스트할 수 있게 None으로 설정한다
    @Value("${app.cookie.refresh-same-site:Lax}")
    private String refreshSameSite;

    //  RefreshToken 을 쿠키에 저장.
    public ResponseCookie createRefreshCookie(String refreshToken) {
        return refreshCookie(refreshToken, Duration.ofMillis(Long.parseLong(refreshExpire)));
    }

    // 쿠키에 저장된 RefreshToken을 삭제. 발급할 때와 같은 속성이어야 브라우저가 같은 쿠키로 인식한다
    public ResponseCookie deleteRefreshCookie() {
        return refreshCookie("", Duration.ZERO);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("RefreshToken", value)
            .httpOnly(true)
            .secure(true)
            .sameSite(refreshSameSite)
            .path("/")
            .maxAge(maxAge)
            .build();
    }

}
