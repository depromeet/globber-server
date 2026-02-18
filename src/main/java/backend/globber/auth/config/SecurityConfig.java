package backend.globber.auth.config;

import backend.globber.auth.config.oauth.CustomOAuth2AuthorizationRequestResolver;
import backend.globber.auth.config.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.CorsFilter;

import static org.springframework.security.config.Customizer.withDefaults;


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
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new CustomOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
            CustomOAuth2AuthorizationRequestResolver customResolver,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authRequestRepository
    ) throws Exception {

        AuthorizationFilter authorizationFilter = new AuthorizationFilter(jwtTokenProvider);

        http
                .addFilterBefore(new FilterExceptionHandler(new ObjectMapper()), CorsFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(a -> a
                                .authorizationRequestResolver(customResolver)
                                .authorizationRequestRepository(authRequestRepository)
                        )
                        .tokenEndpoint(t -> t.accessTokenResponseClient(accessTokenResponseClient))
                        .userInfoEndpoint(u -> u.userService(oauthUtil))
                        .failureHandler(oauthUtil::oauthFailureHandler)
                        .successHandler(oauthUtil::oauthSuccessHandler)
                )
                .addFilterAfter(authorizationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}