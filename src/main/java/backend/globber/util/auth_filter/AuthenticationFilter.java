package backend.globber.util.auth_filter;

import backend.globber.dto.request.LoginRequest;
import backend.globber.dto.response.ApiResponse;
import backend.globber.exception.spec.CustomIOException;
import backend.globber.service.TokenService;
import backend.globber.util.CookieProvider;
import backend.globber.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final CookieProvider cookieProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    // todo: specify error handler
    // 인증 시도, HTTPRequest, HTTPResponse를 받아서 인증을 시도하는 메소드
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException{
        log.info("Enter AuthenticationFilter" + request.getRequestURI() + " " + request.getMethod());
        Authentication authentication;
        try {
            // request의 body에서 email, password를 읽어옴
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);

            // AuthenticationManager를 통해 Authentication 객체를 생성
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.email(),
                    loginRequest.password())
            );
        } catch (IOException e) {
            throw new CustomIOException();
        }
        return authentication;
    }

    // 인증(attemptAuthentication) 성공 시 실행되는 메소드
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult){
        // Authentication 객체에서 email을 추출
        String email = authResult.getName();

        // Authentication 객체에서 권한을 추출
        List<String> roles = authResult.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        // email을 이용해 AccessToken, RefreshToken을 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, roles);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // RefreshToken을 Redis에 저장
        tokenService.updateRefreshToken(email, refreshToken);

        // RefreshToken을 쿠키에 저장
        ResponseCookie refreshCookie = cookieProvider.createRefreshCookie(refreshToken);
        response.addCookie(cookieProvider.of(refreshCookie));

        // AccessToken을 Response Header에 저장
        response.addHeader("Authorization", accessToken);
        // Response Body에 로그인 성공 메시지 반환
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.success("로그인 성공")));
        } catch (IOException e) {
            throw new CustomIOException();
        }
    }

    // AuthenticationException 을 트리거로 하는 에러 메시지 반환
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            // 로그인 실패 메시지 반환
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("로그인 실패")));
        } catch (IOException e) {
            throw new CustomIOException();
        }
    }
}
