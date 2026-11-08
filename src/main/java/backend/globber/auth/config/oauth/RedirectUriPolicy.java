package backend.globber.auth.config.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * 로그인 후 돌아갈 주소(redirect_uri)를 허용 목록의 출처(scheme + host + port)와 비교해 검사한다.
 * 문자열 접두어로 비교하지 않아 https://globber.world.evil.example 같은 주소를 막는다.
 */
@Slf4j
@Component
public class RedirectUriPolicy {

    private final List<Origin> allowedOrigins;
    private final String defaultRedirectUri;

    public RedirectUriPolicy(
        @Value("${oauth.redirect.allowed}") List<String> allowedOrigins,
        @Value("${oauth2_redirect_uri.success}") String defaultRedirectUri
    ) {
        this.allowedOrigins = allowedOrigins.stream()
            .map(String::trim)
            .map(Origin::parse)
            .filter(Objects::nonNull)
            .toList();
        this.defaultRedirectUri = defaultRedirectUri;
    }

    public boolean isAllowed(String redirectUri) {
        Origin origin = Origin.parse(redirectUri);
        return origin != null && allowedOrigins.contains(origin);
    }

    /**
     * 허용된 출처면 받은 주소를, 아니면 기본 콜백 주소(oauth2_redirect_uri.success)를 돌려준다.
     */
    public String resolve(String redirectUri) {
        if (isAllowed(redirectUri)) {
            return redirectUri;
        }
        if (redirectUri != null && !redirectUri.isBlank()) {
            log.warn("허용되지 않은 redirect_uri라 기본 콜백 주소로 이동 - redirectUri: {}", redirectUri);
        }
        return defaultRedirectUri;
    }

    /**
     * OAuth 콜백 처리 중 저장소가 꺼내 둔 인가 요청에서 redirect_uri를 읽어 이동할 주소를 정한다.
     */
    public String resolveFromCallback(HttpServletRequest request) {
        Object removed = request.getAttribute(RedisOAuth2AuthorizationRequestRepository.REMOVED_REQUEST_ATTR);
        String stored = null;
        if (removed instanceof OAuth2AuthorizationRequest authorizationRequest) {
            Object value = authorizationRequest.getAttributes()
                .get(CustomOAuth2AuthorizationRequestResolver.REDIRECT_URI_ATTR);
            if (value != null) {
                stored = value.toString();
            }
        }
        return resolve(stored);
    }

    private record Origin(String scheme, String host, int port) {

        static Origin parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                URI uri = new URI(value);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (scheme == null || host == null || uri.getRawUserInfo() != null) {
                    return null;
                }
                scheme = scheme.toLowerCase(Locale.ROOT);
                if (!scheme.equals("http") && !scheme.equals("https")) {
                    return null;
                }
                int port = uri.getPort() != -1 ? uri.getPort() : (scheme.equals("https") ? 443 : 80);
                return new Origin(scheme, host.toLowerCase(Locale.ROOT), port);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
