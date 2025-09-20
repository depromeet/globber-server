package backend.globber.auth.util.auth_filter;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.dto.OAuthAttributeDto;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.SecurityUserDetailService;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.CustomIOException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthUtil implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final SecurityUserDetailService userDetailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final CookieProvider cookieProvider;

    @Value("${oauth2_redirect_uri.success}")
    private String successRedirectUri;
    @Value("${oauth2_redirect_uri.failure}")
    private String failureRedirectUri;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("Loading user");
        // 소셜 로그인 정보 추출
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        AuthProvider provider = null;
        // 추후 다른 소셜 로그인 추가시 case문 추가.
        switch (userRequest.getClientRegistration().getRegistrationId()) {
            case "kakao":
                provider = AuthProvider.KAKAO;
                break;
            default:
                throw new CustomAuthException("지원하지 않는 OAuth2 제공자입니다.");
        }

        // 소셜 로그인 정보 추출 -> userNameAttributeName은 소셜 로그인을 통해 가져온 사용자 정보 중 username으로 지정할 값.
        // 우리는 이 userNameAttributeName에 해당하는값 + provider로 email을 만들어줄 예정이다.
        String userNameAttributeName = userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

        // 소셜 로그인 정보 추출
        OAuthAttributeDto oAuthAttributeDto = OAuthAttributeDto.of(oAuth2User.getAttributes(),
            userNameAttributeName, provider);
        Member member = oAuthAttributeDto.toEntity();
        // 이미 가입된 회원인지 확인
        if (!memberRepository.existsByEmail(member.getEmail())) {
            memberRepository.save(member);
        }
        // member의 Role 을 이용해 authorities를 구성.
        Collection<? extends GrantedAuthority> authorities = userDetailService.loadUserByUsername(
            member.getEmail()).getAuthorities();
        return new DefaultOAuth2User(authorities, oAuthAttributeDto.getAttributes(),
            userNameAttributeName);
    }

    public void oauthSuccessHandler(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) {

        log.info("Oauth success handler");
        // 받아온 Authentication을 OAuth2AuthenticationToken으로 캐스팅
        // 그 후에 provider 추출.
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
        String email = authentication.getName() + "@" + provider;

        // 리프레시 토큰
        String refreshToken = jwtTokenProvider.createRefreshToken();
        tokenService.updateRefreshToken(email, refreshToken);
        // 쿠키 달기
        ResponseCookie responseCookie = cookieProvider.createRefreshCookie(refreshToken);
        Cookie cookie = cookieProvider.of(responseCookie);
        response.addCookie(cookie);
        response.setContentType("application/json");

        // 권한 추출 from Authentication
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, roles);
        response.addHeader("Authorization", accessToken);
        try {
            // 리다이렉트
            response.sendRedirect(successRedirectUri + accessToken);
        } catch (IOException e) {
            throw new CustomAuthException();
        }

    }

    // 실패시 로그인 페이지로 리다이렉트.
    public void oauthFailureHandler(HttpServletRequest request, HttpServletResponse response,
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
