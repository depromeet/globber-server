package backend.globber.auth.util.auth_filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import backend.globber.auth.config.oauth.RedirectUriPolicy;
import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.auth.util.LoginCodeStore;
import backend.globber.auth.util.LoginCodeStore.LoginResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@ExtendWith(MockitoExtension.class)
class OauthSuccessHandlerTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private CookieProvider cookieProvider;
    @Mock
    private RedirectUriPolicy redirectUriPolicy;
    @Mock
    private LoginCodeStore loginCodeStore;
    @Mock
    private Member member;

    @InjectMocks
    private OauthUtil oauthUtil;

    @Test
    @DisplayName("로그인 성공 시 리다이렉트에는 일회용 code만 붙이고, 토큰은 URL·헤더에 싣지 않으며 쿠키 속성을 그대로 내려준다")
    void success_redirectsWithCodeOnly() throws Exception {
        // given
        var authentication = new OAuth2AuthenticationToken(
            new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("id", "12345"), "id"),
            List.of(new SimpleGrantedAuthority("ROLE_USER")), "kakao");
        given(memberRepository.findByEmail("12345@kakao")).willReturn(Optional.of(member));
        given(member.getId()).willReturn(7L);
        given(member.getUuid()).willReturn("abc123");
        given(member.isFirstLogin()).willReturn(true);
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh");
        given(jwtTokenProvider.createAccessToken(eq("12345@kakao"), anyList())).willReturn("Bearer access");
        given(cookieProvider.createRefreshCookie("refresh")).willReturn(ResponseCookie.from("RefreshToken", "refresh")
            .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(Duration.ofDays(30)).build());
        given(redirectUriPolicy.resolveFromCallback(any())).willReturn("https://globber.world/login/oauth/success");
        given(loginCodeStore.issue(any())).willReturn("one-time-code");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        oauthUtil.oauthSuccessHandler(new MockHttpServletRequest(), response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("https://globber.world/login/oauth/success?code=one-time-code");
        assertThat(response.getRedirectedUrl()).doesNotContain("access").doesNotContain("uuid");
        assertThat(response.getHeader("Authorization")).isNull();
        assertThat(response.getHeader("Set-Cookie"))
            .contains("RefreshToken=refresh").contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
        verify(loginCodeStore).issue(new LoginResult("Bearer access", 7L, "abc123", true));
        verify(tokenService).updateRefreshToken(anyString(), eq("refresh"));
    }
}
