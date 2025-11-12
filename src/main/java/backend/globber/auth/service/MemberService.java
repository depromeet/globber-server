package backend.globber.auth.service;

import backend.globber.auth.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberService {

    private final MemberSaver memberSaver;

    private static final int MAX_RETRY = 8;

    public Member registerOAuthMember(Member newMember) {
        int tries = 0;
        while (true) {
            try {
                return memberSaver.saveWithUUID(newMember);
            } catch (DataIntegrityViolationException e) {
                if (++tries < MAX_RETRY) continue;
                throw e;
            }
        }
    }
}

