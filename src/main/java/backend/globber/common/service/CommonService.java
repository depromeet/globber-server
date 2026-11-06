package backend.globber.common.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.ForbiddenException;
import backend.globber.exception.spec.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    // AccessToken에서 MemberId 추출
    public Long getMemberIdFromToken(String accessToken) {
        String email = jwtTokenProvider.getEmailForAccessToken(accessToken);
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));
        return member.getId();
    }

    // AccessToken을 검증하고, DB에 저장된 현재 역할이 관리자일 때만 MemberId 반환
    public Long getAdminMemberIdFromToken(String accessToken) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new CustomAuthException("인증이 필요합니다.");
        }
        jwtTokenProvider.validateToken(accessToken);
        Member member = memberRepository.findByEmail(jwtTokenProvider.getEmailForAccessToken(accessToken))
            .orElseThrow(() -> new CustomAuthException("존재하지 않는 회원입니다."));
        if (!member.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new ForbiddenException("관리자 권한이 필요합니다.");
        }
        return member.getId();
    }
}
