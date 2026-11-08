package backend.globber.auth.config.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;

class RedirectUriPolicyTest {

    private static final String SUCCESS = "https://globber.world/login/oauth/success";

    private final RedirectUriPolicy policy = new RedirectUriPolicy(
        List.of("https://globber.world", "http://localhost:3000"), SUCCESS);

    @ParameterizedTest
    @ValueSource(strings = {
        "https://globber.world/login/oauth/success",
        "https://GLOBBER.world/login/oauth/success",
        "https://globber.world:443/login/oauth/success?from=bookmark",
        "http://localhost:3000/login/oauth/success"
    })
    @DisplayName("허용 목록과 출처가 같으면 받은 주소를 그대로 쓴다")
    void allowedOrigin_keepsUri(String uri) {
        // when
        String resolved = policy.resolve(uri);

        // then
        assertThat(policy.isAllowed(uri)).isTrue();
        assertThat(resolved).isEqualTo(uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://evil.example/steal",
        "https://globber.world.evil.example/login/oauth/success",
        "https://globber.world@evil.example/login/oauth/success",
        "http://globber.world/login/oauth/success",
        "https://www.globber.world/login/oauth/success",
        "http://localhost:3001/login/oauth/success",
        "javascript:alert(1)",
        "//evil.example/steal",
        "/login/oauth/success",
        "https://glob ber.world/%%%"
    })
    @DisplayName("출처가 다르거나 해석할 수 없는 주소는 기본 콜백 주소로 바꾼다")
    void disallowedOrInvalid_fallsBackToSuccessUri(String uri) {
        // when
        String resolved = policy.resolve(uri);

        // then
        assertThat(policy.isAllowed(uri)).isFalse();
        assertThat(resolved).isEqualTo(SUCCESS);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("주소가 없으면 기본 콜백 주소를 쓴다")
    void missing_fallsBackToSuccessUri(String uri) {
        // when & then
        assertThat(policy.resolve(uri)).isEqualTo(SUCCESS);
    }

    private MockHttpServletRequest callbackWithRemovedRequest(String storedRedirectUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> attributes = storedRedirectUri == null
            ? Map.of("registration_id", "kakao")
            : Map.of("registration_id", "kakao",
                CustomOAuth2AuthorizationRequestResolver.REDIRECT_URI_ATTR, storedRedirectUri);
        request.setAttribute(RedisOAuth2AuthorizationRequestRepository.REMOVED_REQUEST_ATTR,
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .attributes(attributes)
                .build());
        return request;
    }

    @Test
    @DisplayName("콜백에서 꺼낸 인가 요청에 허용된 redirect_uri가 있으면 그 주소로 보낸다")
    void resolveFromCallback_allowed() {
        // given
        MockHttpServletRequest request = callbackWithRemovedRequest("http://localhost:3000/login/oauth/success");

        // when & then
        assertThat(policy.resolveFromCallback(request)).isEqualTo("http://localhost:3000/login/oauth/success");
    }

    @Test
    @DisplayName("콜백에서 꺼낸 redirect_uri가 허용되지 않았거나 없으면 기본 콜백 주소로 보낸다")
    void resolveFromCallback_disallowedOrMissing() {
        // given
        MockHttpServletRequest disallowed = callbackWithRemovedRequest("https://evil.example/steal");
        MockHttpServletRequest withoutUri = callbackWithRemovedRequest(null);
        MockHttpServletRequest withoutRequest = new MockHttpServletRequest();

        // when & then
        assertThat(policy.resolveFromCallback(disallowed)).isEqualTo(SUCCESS);
        assertThat(policy.resolveFromCallback(withoutUri)).isEqualTo(SUCCESS);
        assertThat(policy.resolveFromCallback(withoutRequest)).isEqualTo(SUCCESS);
    }

    @Test
    @DisplayName("쉼표로 구분한 허용 목록 설정값을 목록으로 읽는다")
    void commaSeparatedAllowedList_isBound() {
        new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withPropertyValues(
                "oauth.redirect.allowed=https://dev.globber.world,http://localhost:3000",
                "oauth2_redirect_uri.success=https://dev.globber.world/login/oauth/success")
            .withBean(RedirectUriPolicy.class)
            .run(context -> {
                // given
                RedirectUriPolicy bound = context.getBean(RedirectUriPolicy.class);

                // when & then
                assertThat(bound.isAllowed("https://dev.globber.world/login/oauth/success")).isTrue();
                assertThat(bound.isAllowed("http://localhost:3000/login/oauth/success")).isTrue();
                assertThat(bound.isAllowed("https://globber.world/login/oauth/success")).isFalse();
            });
    }
}
