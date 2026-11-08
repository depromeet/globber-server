package backend.globber.auth.util.auth_filter;

import backend.globber.auth.config.oauth.RedirectUriPolicy;
import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.dto.OAuthAttributeDto;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.MemberService;
import backend.globber.auth.service.SecurityUserDetailService;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.bookmark.service.BookmarkService;
import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.CustomIOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthUtil implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final SecurityUserDetailService userDetailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final CookieProvider cookieProvider;
    private final BookmarkService bookmarkService;
    private final MemberService memberService;
    private final RedirectUriPolicy redirectUriPolicy;

    @Value("${oauth2_redirect_uri.failure}")
    private String failureRedirectUri;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        AuthProvider provider;

        switch (userRequest.getClientRegistration().getRegistrationId()) {
            case "kakao":
                provider = AuthProvider.KAKAO;
                break;
            default:
                throw new CustomAuthException("지원하지 않는 OAuth2 제공자입니다.");
        }

        String userNameAttributeName = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributeDto oAuthAttributeDto =
                OAuthAttributeDto.of(oAuth2User.getAttributes(), userNameAttributeName, provider);

        Member member = oAuthAttributeDto.toEntity();

        if (!memberRepository.existsByEmail(member.getEmail())) {
            memberService.registerOAuthMember(member);
        }

        Collection<? extends GrantedAuthority> authorities =
                userDetailService.loadUserByUsername(member.getEmail()).getAuthorities();

        return new DefaultOAuth2User(authorities,
                oAuthAttributeDto.getAttributes(),
                userNameAttributeName);
    }


    public void oauthSuccessHandler(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        String email = authentication.getName() + "@" + provider;

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomAuthException("회원이 존재하지 않습니다."));

        String refreshToken = jwtTokenProvider.createRefreshToken();
        tokenService.updateRefreshToken(email, refreshToken);

        ResponseCookie responseCookie = cookieProvider.createRefreshCookie(refreshToken);
        response.addCookie(cookieProvider.of(responseCookie));

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtTokenProvider.createAccessToken(email, roles);
        response.addHeader("Authorization", accessToken);

        try {
            // 콜백에서 꺼낸 인가 요청의 redirect_uri가 허용 출처일 때만 쓰고, 아니면 기본 콜백 주소로 보낸다
            String redirectUri = redirectUriPolicy.resolveFromCallback(request);

            response.sendRedirect(
                    redirectUri
                            + (redirectUri.contains("?") ? "&" : "?")
                            + "accessToken=" + accessToken
                            + "&uuid=" + member.getUuid()
                            + "&firstLogin=" + member.isFirstLogin()
            );

        } catch (IOException e) {
            throw new CustomAuthException();
        }
    }


    public void oauthFailureHandler(HttpServletRequest request,
                                    HttpServletResponse response,
                                    AuthenticationException exception) {

        log.error("OAuth2 login failed", exception);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        try {
            response.sendRedirect(failureRedirectUri);
        } catch (IOException e) {
            throw new CustomIOException(exception.getMessage());
        }
    }
}
