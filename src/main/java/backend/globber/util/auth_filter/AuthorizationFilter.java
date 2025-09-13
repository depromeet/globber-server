package backend.globber.util.auth_filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import backend.globber.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
@Slf4j
public class AuthorizationFilter extends OncePerRequestFilter{
    private final JwtTokenProvider jwtTokenProvider;
    // 필터링을 통해 요청이 들어왔을 때, 해당 요청이 허용되는지 확인하는 메소드
    // 토큰 검사후, @Secured를 위해 Authentication 객체 발행.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {


        String uri = request.getRequestURI();


        // 화이트리스트 경로 (OAuth2, Swagger, API docs 등)
        if (uri.startsWith("/login/oauth2")
            || uri.startsWith("/oauth2")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/swagger-resources")
            || uri.startsWith("/webjars")
            || uri.startsWith("/oauth/token")) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

            if (jwtTokenProvider.validateToken(token)) {
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                for (String role : jwtTokenProvider.getRole(token)) {
                    authorities.add((GrantedAuthority) () -> role);
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    jwtTokenProvider.getEmailForAccessToken(token), null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }
}