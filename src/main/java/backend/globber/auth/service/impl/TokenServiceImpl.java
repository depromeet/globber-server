package backend.globber.auth.service.impl;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.RefreshToken;
import backend.globber.auth.dto.response.JwtTokenResponse;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.repository.RefreshTokenRedisRepository;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.AccessTokenBlacklist;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.CustomTokenException;
import backend.globber.exception.spec.UsernameNotFoundException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final AccessTokenBlacklist accessTokenBlacklist;

    // todo: specify error handler
    @Override
    public void updateRefreshToken(String email, String refreshTokenID) {
        // 사용자 체크
        if (memberRepository.existsByEmail(email)) {
            refreshTokenRepository.save(RefreshToken.of(email, refreshTokenID));
        } else {
            throw new UsernameNotFoundException("해당 이메일을 가진 사용자가 존재하지 않습니다.");
        }
    }

    // todo: specify error handler
    @Override
    public JwtTokenResponse updateAccessToken(String accessToken, String refreshToken) {
        // 재발급은 만료된 액세스 토큰으로 요청하므로 서명만 검증하고 만료는 허용한다
        Claims accessClaims = jwtTokenProvider.getClaimsAllowingExpired(
                jwtTokenProvider.resolveBearerToken(accessToken));
        // 사용자 체크
        Member member = memberRepository.findByEmail(accessClaims.getSubject()).orElseThrow(
                () -> new UsernameNotFoundException("해당 이메일을 가진 사용자가 존재하지 않습니다.")
        );
        // 토큰 존재여부 체크
        RefreshToken refreshTokenEntity = refreshTokenRepository.findById(member.getEmail())
                .orElseThrow(
                        () -> new CustomTokenException("해당 이메일을 가진 사용자의 RefreshToken이 존재하지 않습니다.")
                );
        // 토큰 유효여부 체크
        jwtTokenProvider.validateToken(refreshToken);

        // 토큰 동일여부 체크
        if (!refreshTokenEntity.matches(refreshToken)) {
            throw new CustomTokenException("RefreshToken이 일치하지 않습니다.");
        }
        // 토큰 갱신
        List<String> roles = accessClaims.get("role", List.class);
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getEmail(), roles);
        return JwtTokenResponse.toResponse(newAccessToken, refreshToken);
    }

    @Override
    public void logout(String accessToken) {
        // Bearer 형식 확인 및 토큰 유효여부 체크 (이미 로그아웃한 토큰이면 여기서 401)
        String rawToken = jwtTokenProvider.resolveBearerToken(accessToken);
        Claims claims = jwtTokenProvider.getClaims(rawToken);
        // RefreshToken이 없어도 로그아웃은 성공으로 처리한다
        refreshTokenRepository.findById(claims.getSubject()).ifPresent(refreshTokenRepository::delete);
        // 남은 만료 시간 동안 같은 액세스 토큰을 쓰지 못하게 한다
        accessTokenBlacklist.add(rawToken, claims.getExpiration());
    }

    @Override
    public Long getMemberIdFromAccessToken(String accessToken) {
        // Bearer 형식 확인 및 토큰 유효여부 체크
        if (!jwtTokenProvider.validateToken(jwtTokenProvider.resolveBearerToken(accessToken))) {
            throw new CustomTokenException("토큰이 유효하지 않습니다.");
        }
        // 사용자 체크
        Member member = memberRepository.findByEmail(
                jwtTokenProvider.getEmailForAccessToken(accessToken)).orElseThrow(
                () -> new UsernameNotFoundException("해당 이메일을 가진 사용자가 존재하지 않습니다.")
        );
        return member.getId();
    }

    @Override
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomAuthException("해당 회원을 찾을 수 없습니다."));

        String randomSuffix = UUID.randomUUID().toString();
        String newEmail = "deleted_" + randomSuffix + "@deleted.com";
        member.updateEmail(newEmail);
        member.setDeleted(true);
        member.setDeletedAt(LocalDateTime.now());
    }
}
