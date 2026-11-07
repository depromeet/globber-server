package backend.globber.common.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
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
        // Bearer 형식이 아니면 401, 서명·만료 검증 실패도 401
        String email = jwtTokenProvider.getEmailForAccessToken(jwtTokenProvider.resolveBearerToken(accessToken));
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));
        return member.getId();
    }
}
