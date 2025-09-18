package backend.globber.auth.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

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
        refreshToken.refreshTokenId = refreshTokenId;
        return refreshToken;
    }
}
