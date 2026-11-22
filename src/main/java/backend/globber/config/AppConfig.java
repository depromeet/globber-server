package backend.globber.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class AppConfig {

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    // FE가 실제로 보내는 요청 헤더만 허용한다(GLB-141). sentry-trace·baggage는 FE Sentry 추적 헤더다.
    private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type", "sentry-trace", "baggage");

    /**
     * 허용 목록에 있는 Origin만 credentials와 함께 허용한다(GLB-141).
     * 목록은 app.cors.allowed-origins이고, 없으면 환경별 oauth.redirect.allowed를 쓴다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:${oauth.redirect.allowed}}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
