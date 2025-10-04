package backend.globber.auth.util.auth_filter;

import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.exception.spec.CustomAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class AuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 필터링을 통해 요청이 들어왔을 때, 해당 요청이 허용되는지 확인하는 메소드
    // 토큰 검사후, @Secured를 위해 Authentication 객체 발행.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain)
        throws IOException, ServletException {

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
            else{
                throw new CustomAuthException("유효하지 않은 토큰입니다.");
            }
        }

        chain.doFilter(request, response);
    }
}