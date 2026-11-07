package backend.globber.auth.util;

import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.CustomTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final String JwtPrefix = "Bearer ";
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.access_expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh_expiration}")
    private long refreshTokenExpiration;

    public String fromHeader(String header) {
        return header.replace(JwtPrefix, "");
    }

    // Authorization 헤더가 "Bearer <토큰>" 형식일 때만 토큰을 꺼낸다. 아니면 401
    public String resolveBearerToken(String header) {
        if (header == null || !header.startsWith(JwtPrefix) || header.length() == JwtPrefix.length()) {
            throw new CustomAuthException("로그인이 필요합니다.");
        }
        return header.substring(JwtPrefix.length());
    }

    public String createAccessToken(String email, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", roles);

        Date now = new Date();
        return JwtPrefix + Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + accessTokenExpiration))
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    public String createRefreshToken() {
        Claims claims = Jwts.claims();
        claims.put("value", UUID.randomUUID().toString());

        // 쿠키에는 공백이 들어갈 수 없으므로 공백 제거 (Bearer 포함 X)
        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(now.getTime() + refreshTokenExpiration))
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    // 토큰의 서브젝트인 이메일 추출
    public String getEmailForAccessToken(String a_token) {
        String token = fromHeader(a_token);
        return getClaims(token).getSubject();
    }

    // 토큰의 클레임 추출 (서명·만료 모두 검증)
    public Claims getClaims(String token) {
        return parseClaims(token, false);
    }

    // 재발급 전용: 서명은 검증하고 만료만 허용한다
    public Claims getClaimsAllowingExpired(String token) {
        return parseClaims(token, true);
    }

    private Claims parseClaims(String token, boolean allowExpired) {
        if (token.startsWith(JwtPrefix)) {
            token = fromHeader(token);
        }
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            // 만료 검사는 서명 없는 토큰 거부보다 먼저 일어나므로, 서명 검증을 통과한 JWS일 때만 claims를 허용한다
            if (allowExpired && e.getHeader() instanceof JwsHeader) {
                return e.getClaims();
            }
            throw new CustomTokenException("만료된 JWT 토큰입니다.");
        } catch (SignatureException e) {
            throw new CustomTokenException("유효하지 않은 JWT 서명입니다.");
        } catch (MalformedJwtException e) {
            throw new CustomTokenException("유효하지 않은 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            throw new CustomTokenException("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            throw new CustomTokenException("JWT 토큰이 비어있습니다.");
        }
    }

    public String getRefreshTokenId(String r_token) {
        return getClaims(fromHeader(r_token)).get("value").toString();
    }

    public Date getExpirationTime(String token) {
        if (token.startsWith(JwtPrefix)) {
            token = fromHeader(token);
        }
        return getClaims(token).getExpiration();
    }

    public List<String> getRole(String a_token) {
        return getClaims(a_token).get("role", List.class);
    }

    public boolean validateToken(String token) {
        parseClaims(token, false);
        return true;
    }
}