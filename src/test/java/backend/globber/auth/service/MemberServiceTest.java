package backend.globber.auth.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.ShortIdGenerator;
import backend.globber.support.PostgresTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import({MemberService.class, ShortIdGenerator.class, MemberSaver.class, PostgresTestConfig.class})
class MemberServiceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private ShortIdGenerator shortIdGenerator;

    @Autowired
    private MemberSaver memberSaver;

    @Autowired
    EntityManager em;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("신규 회원이 정상 등록된다")
    @Rollback
    void registerNewMember_success() {
        // given
        given(shortIdGenerator.generate(6)).willReturn("AAAAAA");

        Member newMember = Member.of(
                "test@example.com", "테스트유저", null,
                AuthProvider.KAKAO, List.of(Role.ROLE_USER), null
        );

        // when
        Member saved = memberService.registerOAuthMember(newMember);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getUuid()).isNotNull().hasSize(6);
        assertThat(memberRepository.count()).isEqualTo(1);
    }
}
