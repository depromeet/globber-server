package backend.globber.config;

import backend.globber.exception.FilterExceptionHandler;
import backend.globber.service.TokenService;
import backend.globber.util.CookieProvider;
import backend.globber.util.JwtTokenProvider;
import backend.globber.util.OauthUtil;
import backend.globber.util.auth_filter.AuthenticationFilter;
import backend.globber.util.auth_filter.AuthorizationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsFilter corsFilter;
    private final CookieProvider cookieProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final OauthUtil oauthUtil;
    private final TokenService tokenService;

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        // RestClient 빌더에 로깅 인터셉터 추가
        RestClient restClient = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                System.out.println("===== [Token Request] =====");
                System.out.println(request.getMethod() + " " + request.getURI());
                request.getHeaders().forEach((name, values) ->
                    values.forEach(value -> System.out.println(name + ": " + value))
                );
                return execution.execute(request, body);
            })
            .build();

        RestClientAuthorizationCodeTokenResponseClient client =
            new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(restClient);

        return client;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        // 인증 필터 생성 및 의존성 주입
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(
            authenticationManager, cookieProvider, jwtTokenProvider, tokenService, new ObjectMapper()
        );
        authenticationFilter.setFilterProcessesUrl("/login");

        AuthorizationFilter authorizationFilter = new AuthorizationFilter(jwtTokenProvider);

        http
            // 예외 처리 필터
            .addFilterBefore(new FilterExceptionHandler(new ObjectMapper()), CorsFilter.class)
            // CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // 세션 사용 안함
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 폼 로그인 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            // 로그아웃 비활성화
            .logout(AbstractHttpConfigurer::disable)
            // OAuth2 로그인
            .oauth2Login(oauth -> oauth
                .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient()))
                .userInfoEndpoint(c -> c.userService(oauthUtil))
                .failureHandler(oauthUtil::oauthFailureHandler)
                .successHandler(oauthUtil::oauthSuccessHandler)
            )
            // CORS 필터 추가
            .addFilter(corsFilter)
            // JWT 인증 필터
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // JWT 인가 필터
            .addFilterBefore(authorizationFilter, AuthenticationFilter.class);

        return http.build();
    }

}
