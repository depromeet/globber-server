package backend.globber.auth.config.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class CustomOAuth2AuthorizationRequestResolverTest {

    private final CustomOAuth2AuthorizationRequestResolver resolver = new CustomOAuth2AuthorizationRequestResolver(
        new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("kakao")
            .clientId("client-id")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://globber.world/login/oauth2/code/kakao")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v2/user/me")
            .userNameAttributeName("id")
            .build()),
        "/oauth2/authorization",
        new RedirectUriPolicy(List.of("https://globber.world"), "https://globber.world/login/oauth/success"));

    private MockHttpServletRequest loginStart(String redirectUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        request.setServletPath("/oauth2/authorization/kakao");
        request.setParameter("redirect_uri", redirectUri);
        return request;
    }

    @Test
    @DisplayName("허용된 출처의 redirect_uri는 인가 요청 속성에 저장한다")
    void allowedRedirectUri_isStored() {
        // when
        OAuth2AuthorizationRequest request = resolver.resolve(loginStart("https://globber.world/login/oauth/success"));

        // then
        assertThat(request.getAttributes())
            .containsEntry(CustomOAuth2AuthorizationRequestResolver.REDIRECT_URI_ATTR,
                "https://globber.world/login/oauth/success");
    }

    @Test
    @DisplayName("허용되지 않은 출처의 redirect_uri는 저장하지 않는다")
    void disallowedRedirectUri_isNotStored() {
        // when
        OAuth2AuthorizationRequest request = resolver.resolve(loginStart("https://evil.example/steal"));

        // then
        assertThat(request).isNotNull();
        assertThat(request.getAttributes())
            .doesNotContainKey(CustomOAuth2AuthorizationRequestResolver.REDIRECT_URI_ATTR);
    }
}
