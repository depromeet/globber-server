package backend.globber.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.MemberSaver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.mock.env.MockEnvironment;

class TestAccountInitializerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberSaver memberSaver = mock(MemberSaver.class);

    private TestAccountProperties properties(String... emails) {
        TestAccountProperties properties = new TestAccountProperties();
        properties.setTestAccounts(java.util.Arrays.stream(emails).map(email -> {
            TestAccountProperties.Account account = new TestAccountProperties.Account();
            account.setEmail(email);
            account.setNickname("테스트");
            return account;
        }).toList());
        return properties;
    }

    @Test
    @DisplayName("계정 조회가 DB 오류로 실패해도 앱 기동을 막지 않고 다음 계정으로 넘어간다")
    void lookupFailure_doesNotBlockStartup() {
        // given: 운영 프로필, 키 설정, 아직 테이블이 없는 DB
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        given(memberRepository.existsByEmail("first@globber.world"))
            .willThrow(new InvalidDataAccessResourceUsageException("relation \"member\" does not exist"));
        given(memberRepository.existsByEmail("second@globber.world")).willReturn(true);
        CommandLineRunner runner = new TestAccountInitializer().initTestAccounts(
            memberRepository, memberSaver, properties("first@globber.world", "second@globber.world"),
            environment, "secret-key");

        // when & then
        assertThatCode(() -> runner.run()).doesNotThrowAnyException();
        verify(memberRepository).existsByEmail("second@globber.world");
    }
}
