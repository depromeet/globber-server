package backend.globber.auth.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.ShortIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final ShortIdGenerator shortIdGenerator;

    private static final int UUID_LENGTH = 6;
    private static final int MAX_RETRY = 8;

    @Transactional
    public Member registerOAuthMember(Member newMember) {
        Optional<Member> existingOpt = memberRepository.findByEmailIncludingDeleted(newMember.getEmail());
        if (existingOpt.isPresent()) {
            Member existing = existingOpt.get();
            if (existing.isDeleted()) {
                existing.restore();
                log.info("Soft-deleted member 복구됨: {}", existing.getEmail());
            }
            return existing;
        }

        int tries = 0;
        while (true) {
            try {
                return saveMemberWithUUID(newMember);
            } catch (DataIntegrityViolationException e) {
                if (++tries < MAX_RETRY) continue;
                throw e;
            }
        }
    }

    @Transactional
    public Member saveMemberWithUUID(Member member) {
        String uuid = shortIdGenerator.generate(UUID_LENGTH);
        member.changeUUID(uuid);
        return memberRepository.save(member);
    }
}

