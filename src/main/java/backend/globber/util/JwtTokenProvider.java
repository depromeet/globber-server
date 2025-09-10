package backend.globber.util;

import backend.globber.exception.spec.CustomTokenException;
import io.jsonwebtoken.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access_expiration}")
    private long accessTokenExpiration;

    private final String JwtPrefix = "Bearer ";
    public String fromHeader(String header) {
        return header.replace(JwtPrefix, "");
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

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    // 토큰의 서브젝트인 이메일 추출
    public String getEmailForAccessToken(String a_token) {
        String token = fromHeader(a_token);
        return getClaimes(token).getSubject();
    }

    // 토큰의 클레임 추출
    public Claims getClaimes(String token) {
        if (token.startsWith(JwtPrefix)){
            token = fromHeader(token);
        }
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        }
        catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getRefreshTokenId(String r_token) {
        return getClaimes(r_token).get("value").toString();
    }

    public Date getExpirationTime(String token) {
        if(token.startsWith(JwtPrefix)){
            token = fromHeader(token);
        }
        return getClaimes(token).getExpiration();
    }

    public List<String> getRole(String a_token) {
        return getClaimes(a_token).get("role", List.class);
    }

    // Token의 UUID 와 RefreshTokenId 비교
    public boolean sameRefreshToken(String r_token, String tokenId) {
        return r_token.equals(tokenId);
    }

    public boolean validateToken(String token) {
        if (token.startsWith(JwtPrefix)){
            token = fromHeader(token);
        }
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        }
        catch (SignatureException e) {
            throw new CustomTokenException("유효하지 않은 JWT 서명입니다.");
        }
        catch (MalformedJwtException e) {
            throw new CustomTokenException("유효하지 않은 JWT 토큰입니다.");
        }
        catch (ExpiredJwtException e) {
            throw new CustomTokenException("만료된 JWT 토큰입니다.");
        }
        catch (UnsupportedJwtException e) {
            throw new CustomTokenException("지원되지 않는 JWT 토큰입니다.");
        }
        catch (IllegalArgumentException e) {
            throw new CustomTokenException("JWT 토큰이 비어있습니다.");
        }
    }
}