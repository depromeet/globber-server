package backend.globber.auth.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.ShortIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberSaver {
    private final MemberRepository repo;
    private final ShortIdGenerator gen;

    private static final int UUID_LENGTH = 6;

    @Transactional(propagation = REQUIRES_NEW)
    public Member saveWithUUID(Member member) {
        log.debug("새 UUID 생성 후 회원 저장 시도: {}", member.getUuid());
        member.changeUUID(gen.generate(UUID_LENGTH));
        return repo.saveAndFlush(member);
    }
}
