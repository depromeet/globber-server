package backend.globber.auth.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@NoArgsConstructor
@RedisHash(value = "RefreshToken", timeToLive = 60 * 60 * 24 * 7)
@Getter
public class RefreshToken {

    @Id
    private String email;
    private String refreshTokenId;

    public static RefreshToken of(String email, String refreshTokenId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.email = email;
        refreshToken.refreshTokenId = hash(refreshTokenId);
        return refreshToken;
    }
    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
            }
        catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token id", e);
            }
        }
}
