package backend.globber.auth.config.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * OAuth 인가 요청을 Redis에 JSON으로 저장한다. 쿠키에는 추측할 수 없는 식별자만 둔다.
 * 인가 요청은 콜백에서 한 번 꺼내면 지워지고, 꺼낸 요청은 성공 핸들러가 읽을 수 있도록 요청 속성에 남긴다.
 */
@Slf4j
public class RedisOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String REMOVED_REQUEST_ATTR =
        RedisOAuth2AuthorizationRequestRepository.class.getName() + ".REMOVED_REQUEST";

    static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final String KEY_PREFIX = "oauth2:auth-request:";
    private static final Duration TTL = Duration.ofMinutes(3);
    private static final int ID_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public RedisOAuth2AuthorizationRequestRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String id = readCookie(request);
        if (id == null) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + id);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, StoredAuthorizationRequest.class).toRequest();
        } catch (Exception e) {
            log.error("OAuth 인가 요청을 읽지 못함", e);
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        String id = newId();
        try {
            String json = objectMapper.writeValueAsString(StoredAuthorizationRequest.from(authorizationRequest));
            redisTemplate.opsForValue().set(KEY_PREFIX + id, json, TTL);
        } catch (Exception e) {
            throw new IllegalStateException("OAuth 인가 요청을 저장하지 못했습니다.", e);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(id, TTL).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        String id = readCookie(request);
        if (id != null) {
            try {
                redisTemplate.delete(KEY_PREFIX + id);
            } catch (Exception e) {
                log.error("OAuth 인가 요청을 지우지 못함", e);
            }
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        }
        if (authorizationRequest != null) {
            request.setAttribute(REMOVED_REQUEST_ATTR, authorizationRequest);
        }
        return authorizationRequest;
    }

    private String newId() {
        byte[] bytes = new byte[ID_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build();
    }

    /**
     * Redis에 저장하는 인가 요청의 JSON 모양. Spring Security가 지원하는 authorization_code 방식만 다룬다.
     */
    record StoredAuthorizationRequest(
        String authorizationUri,
        String clientId,
        String redirectUri,
        Set<String> scopes,
        String state,
        Map<String, Object> additionalParameters,
        String authorizationRequestUri,
        Map<String, Object> attributes
    ) {

        static StoredAuthorizationRequest from(OAuth2AuthorizationRequest request) {
            return new StoredAuthorizationRequest(
                request.getAuthorizationUri(),
                request.getClientId(),
                request.getRedirectUri(),
                request.getScopes(),
                request.getState(),
                request.getAdditionalParameters(),
                request.getAuthorizationRequestUri(),
                request.getAttributes());
        }

        OAuth2AuthorizationRequest toRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(authorizationUri)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .additionalParameters(additionalParameters)
                .authorizationRequestUri(authorizationRequestUri)
                .attributes(attributes)
                .build();
        }
    }
}
