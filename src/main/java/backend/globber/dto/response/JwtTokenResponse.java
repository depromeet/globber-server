package backend.globber.dto.response;

public record JwtTokenResponse (
    String accessToken,
    String refreshToken
) {
    public static JwtTokenResponse toResponse(String accessToken, String refreshToken) {
        return new JwtTokenResponse(accessToken, refreshToken);
    }
}
