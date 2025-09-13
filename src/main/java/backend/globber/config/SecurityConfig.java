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
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
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
    public RestClient restClient() {
        return RestClient.builder()
            .messageConverters((messageConverters) -> {
                messageConverters.clear();
                messageConverters.add(new FormHttpMessageConverter());
                messageConverters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
            })
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
            .requestInterceptor((request, body, execution) -> {
                System.out.println("=== [OAuth2 Code → Token Request] ===");
                System.out.println("URI: " + request.getURI());
                System.out.println("Method: " + request.getMethod());
                System.out.println("Headers: " + request.getHeaders());
                if (body != null) {
                    System.out.println("Body: " + new String(body));
                }
                var response = execution.execute(request, body);
                System.out.println("=== [OAuth2 Code → Token Response] ===");
                System.out.println("Status: " + response.getStatusCode());
                System.out.println("Headers: " + response.getHeaders());
                return response;
            })
            .build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeAccessTokenResponseClient(RestClient restClient) {
        RestClientAuthorizationCodeTokenResponseClient accessTokenResponseClient =
            new RestClientAuthorizationCodeTokenResponseClient();
        accessTokenResponseClient.setRestClient(restClient);
        return accessTokenResponseClient;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationManager authenticationManager,
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient
    ) throws Exception {
        // JWT 인증 필터
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(
            authenticationManager, cookieProvider, jwtTokenProvider, tokenService, new ObjectMapper()
        );
        authenticationFilter.setFilterProcessesUrl("/login");
        // JWT 인가 필터
        AuthorizationFilter authorizationFilter = new AuthorizationFilter(jwtTokenProvider);
        http
            // 예외 처리 필터
            .addFilterBefore(new FilterExceptionHandler(new ObjectMapper()), CorsFilter.class)
            // CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            // 요청 권한 설정 (예시: /auth/**만 허용, 나머지는 인증 필요)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated()
 //               .anyRequest().permitAll()
            )
            // 세션 사용 안함
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 폼 로그인 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            // 로그아웃 비활성화
            .logout(AbstractHttpConfigurer::disable)
            // OAuth2 로그인
            .oauth2Login(oauth -> oauth
                .tokenEndpoint(t -> t.accessTokenResponseClient(accessTokenResponseClient))
                .userInfoEndpoint(u -> u.userService(oauthUtil))
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