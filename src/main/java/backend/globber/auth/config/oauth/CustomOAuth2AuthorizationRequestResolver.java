package backend.globber.auth.config.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String REDIRECT_URI_ATTR = "redirect_uri";

    private final OAuth2AuthorizationRequestResolver delegate;

    public CustomOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            String authorizationRequestBaseUri
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                authorizationRequestBaseUri
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request);
        return customize(request, authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request, clientRegistrationId);
        return customize(request, authRequest);
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) return null;

        String redirectUri = Optional.ofNullable(request.getParameter(REDIRECT_URI_PARAM))
                .map(v -> URLDecoder.decode(v, StandardCharsets.UTF_8))
                .orElse(null);

        if (redirectUri == null || redirectUri.isBlank()) return authRequest;

        Map<String, Object> attrs = new HashMap<>(authRequest.getAttributes());
        attrs.put(REDIRECT_URI_ATTR, redirectUri);

        return OAuth2AuthorizationRequest.from(authRequest)
                .attributes(attrs)
                .build();
    }
}
