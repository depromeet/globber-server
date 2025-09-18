package backend.globber.auth.config;

import static org.springframework.security.config.Customizer.withDefaults;

import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.auth.util.auth_filter.AuthorizationFilter;
import backend.globber.auth.util.auth_filter.OauthUtil;
import backend.globber.exception.FilterExceptionHandler;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final OauthUtil oauthUtil;


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
            .build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeAccessTokenResponseClient(
        RestClient restClient) {
        RestClientAuthorizationCodeTokenResponseClient accessTokenResponseClient =
            new RestClientAuthorizationCodeTokenResponseClient();
        accessTokenResponseClient.setRestClient(restClient);
        return accessTokenResponseClient;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient
    ) throws Exception {
        // JWT 인가 필터
        AuthorizationFilter authorizationFilter = new AuthorizationFilter(jwtTokenProvider);
        http
            // 예외 처리 필터
            .addFilterBefore(new FilterExceptionHandler(new ObjectMapper()), CorsFilter.class)
            // CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            // 요청 권한 설정 (예시: /auth/**만 허용, 나머지는 인증 필요)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
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
            // JWT 인가 필터
            .addFilterAfter(authorizationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}