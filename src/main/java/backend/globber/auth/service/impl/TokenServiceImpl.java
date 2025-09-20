package backend.globber.auth.service.impl;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.RefreshToken;
import backend.globber.auth.dto.response.JwtTokenResponse;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.repository.RefreshTokenRedisRepository;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.exception.spec.CustomTokenException;
import backend.globber.exception.spec.UsernameNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRepository;

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
        // 사용자 체크
        Member member = memberRepository.findByEmail(
            jwtTokenProvider.getEmailForAccessToken(accessToken)).orElseThrow(
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
        if (!jwtTokenProvider.sameRefreshToken(refreshToken,
            refreshTokenEntity.getRefreshTokenId())) {
            throw new CustomTokenException("RefreshToken이 일치하지 않습니다.");
        }
        // 토큰 갱신
        List<String> roles = jwtTokenProvider.getRole(accessToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getEmail(), roles);
        return JwtTokenResponse.toResponse(newAccessToken, refreshToken);
    }

    @Override
    public void logout(String accessToken) {
        // 토큰 유효여부 체크
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomTokenException("토큰이 유효하지 않습니다.");
        }
        // 사용자 체크 및 refreshToken 존재여부 체크
        RefreshToken refreshTokenEntity = refreshTokenRepository.findById(
            jwtTokenProvider.getEmailForAccessToken(accessToken)).orElseThrow(
            () -> new CustomTokenException("해당 이메일을 가진 사용자의 RefreshToken이 존재하지 않습니다.")
        );
        // 토큰 삭제
        refreshTokenRepository.delete(refreshTokenEntity);
    }
}
