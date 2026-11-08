package backend.globber.auth.config.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
class RedisOAuth2AuthorizationRequestRepositoryTest {

    private static final String COOKIE = "OAUTH2_AUTH_REQUEST";
    private static final String KEY_PREFIX = "oauth2:auth-request:";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .clientId("client-id")
            .redirectUri("https://globber.world/login/oauth2/code/kakao")
            .scopes(Set.of("profile_nickname", "account_email"))
            .state("state-123")
            .additionalParameters(Map.of("prompt", "login"))
            .attributes(Map.of("registration_id", "kakao",
                CustomOAuth2AuthorizationRequestResolver.REDIRECT_URI_ATTR,
                "https://globber.world/login/oauth/success"))
            .authorizationRequestUri("https://kauth.kakao.com/oauth/authorize?state=state-123")
            .build();
    }

    private Cookie savedCookie(MockHttpServletResponse response) {
        Cookie cookie = response.getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    @DisplayName("저장하면 Redis에 JSON으로 3분 동안 두고 쿠키에는 무작위 식별자만 둔다")
    void save_storesJsonInRedisAndIdInCookie() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        // then
        Cookie cookie = savedCookie(response);
        String id = cookie.getValue();
        assertThat(id).hasSizeGreaterThanOrEqualTo(32).doesNotContain("rO0AB");
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        assertThat(stored).startsWith("{").contains("state-123");
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + id);
        assertThat(ttl).isBetween(1L, 180L);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Lax")
            .contains("Path=/").contains("Max-Age=180");
    }

    @Test
    @DisplayName("쿠키의 식별자로 저장한 인가 요청을 그대로 다시 읽는다")
    void load_restoresSameRequest() {
        // given
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        OAuth2AuthorizationRequest original = authorizationRequest();
        repository.saveAuthorizationRequest(original, new MockHttpServletRequest(), saveResponse);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(savedCookie(saveResponse));

        // when
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo(original.getState());
        assertThat(loaded.getClientId()).isEqualTo(original.getClientId());
        assertThat(loaded.getRedirectUri()).isEqualTo(original.getRedirectUri());
        assertThat(loaded.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
        assertThat(loaded.getScopes()).isEqualTo(original.getScopes());
        assertThat(loaded.getAdditionalParameters()).isEqualTo(original.getAdditionalParameters());
        assertThat(loaded.getAttributes()).isEqualTo(original.getAttributes());
        assertThat(loaded.getAuthorizationRequestUri()).isEqualTo(original.getAuthorizationRequestUri());
    }

    @Test
    @DisplayName("한 번 꺼내면 Redis 키와 쿠키를 지우고, 꺼낸 요청은 요청 속성에 남긴다")
    void remove_isOneTime() {
        // given
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest(), new MockHttpServletRequest(), saveResponse);
        Cookie cookie = savedCookie(saveResponse);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        // then
        assertThat(removed).isNotNull();
        assertThat(redisTemplate.hasKey(KEY_PREFIX + cookie.getValue())).isFalse();
        assertThat(response.getCookie(COOKIE).getMaxAge()).isZero();
        assertThat(request.getAttribute(RedisOAuth2AuthorizationRequestRepository.REMOVED_REQUEST_ATTR))
            .isSameAs(removed);

        MockHttpServletRequest again = new MockHttpServletRequest();
        again.setCookies(cookie);
        assertThat(repository.loadAuthorizationRequest(again)).isNull();
    }

    @Test
    @DisplayName("쿠키가 없거나 Redis에 없는 식별자면 null을 돌려준다")
    void load_unknown_returnsNull() {
        // given
        MockHttpServletRequest noCookie = new MockHttpServletRequest();
        MockHttpServletRequest unknown = new MockHttpServletRequest();
        unknown.setCookies(new Cookie(COOKIE, "unknown-id"));

        // when & then
        assertThat(repository.loadAuthorizationRequest(noCookie)).isNull();
        assertThat(repository.loadAuthorizationRequest(unknown)).isNull();
    }

    @Test
    @DisplayName("예전 Java 직렬화 쿠키 값은 읽지 않고 null을 돌려준다")
    void load_legacySerializedCookie_returnsNull() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE, "rO0ABXNyAEVvcmcuc3ByaW5nZnJhbWV3b3Jr"));

        // when & then
        assertThat(repository.loadAuthorizationRequest(request)).isNull();
    }
}
