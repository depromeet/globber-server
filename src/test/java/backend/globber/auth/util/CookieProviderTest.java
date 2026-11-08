package backend.globber.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

class CookieProviderTest {

    private CookieProvider provider(String sameSite) {
        CookieProvider provider = new CookieProvider();
        // jwt.refresh_expiration은 밀리초 단위(운영 30일)
        ReflectionTestUtils.setField(provider, "refreshExpire", "2592000000");
        ReflectionTestUtils.setField(provider, "refreshSameSite", sameSite);
        return provider;
    }

    @Test
    @DisplayName("RefreshToken 쿠키는 HttpOnly·Secure·Path=/이고, Max-Age는 리프레시 토큰 수명을 초로 바꾼 값이다")
    void refreshCookie_attributes() {
        // when
        ResponseCookie cookie = provider("Lax").createRefreshCookie("token");

        // then
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(2_592_000L);
    }

    @Test
    @DisplayName("SameSite는 설정값을 따른다 (개발 서버는 None)")
    void refreshCookie_sameSiteFromConfig() {
        // when & then
        assertThat(provider("None").createRefreshCookie("token").getSameSite()).isEqualTo("None");
    }

    @Test
    @DisplayName("삭제 쿠키도 같은 속성으로 Max-Age=0을 내려준다")
    void deleteCookie_sameAttributes() {
        // when
        ResponseCookie cookie = provider("Lax").deleteRefreshCookie();

        // then
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
    }
}
