package backend.globber.auth.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.ShortIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ShortIdGenerator shortIdGenerator;
    private static final int UUID_LENGTH = 6;
    private static final int MAX_RETRY = 8;

    @Transactional
    public Member registerOAuthMember(Member member) {
        int tries = 0;
        while (true) {
            try {
                String uuid = shortIdGenerator.generate(UUID_LENGTH);
                member.changeUUID(uuid);
                return memberRepository.save(member);
            } catch (DataIntegrityViolationException e) {
                if (++tries < MAX_RETRY) continue;
                throw e;
            }
        }
    }
}
