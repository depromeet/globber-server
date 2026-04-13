package backend.globber.config;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.service.MemberSaver;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "local"})
@Slf4j
@EnableConfigurationProperties(TestAccountProperties.class)
public class DevTestAccountInitializer {

    @Bean
    public CommandLineRunner initTestAccounts(
        MemberRepository memberRepository,
        MemberSaver memberSaver,
        TestAccountProperties testAccountProperties) {
        return args -> {

            List<TestAccountProperties.Account> testAccounts = testAccountProperties.getAccounts();

            if (testAccounts == null || testAccounts.isEmpty()) {
                log.warn("설정된 테스트 계정이 없습니다. application.yml의 app.test-accounts를 확인하세요.");
                return;
            }

            for (TestAccountProperties.Account account : testAccounts) {
                if (memberRepository.existsByEmail(account.getEmail())) {
                    log.info("이미 존재함: {}", account.getEmail());
                } else {
                    try {
                        Member member = Member.of(
                            account.getEmail(),
                            account.getNickname(),
                            null,  // password (OAuth 사용자이므로 null)
                            AuthProvider.KAKAO,
                            List.of(Role.ROLE_USER),
                            null  // uuid는 saveWithUUID에서 생성됨
                        );

                        memberSaver.saveWithUUID(member);
                        log.info("테스트 계정 생성 완료: {} (UUID: {})", account.getEmail(), member.getUuid());
                    } catch (Exception e) {
                        log.error("테스트 계정 생성 실패: {} - {}", account.getEmail(), e.getMessage());
                    }
                }
            }

            log.info("=== 테스트 계정 자동 생성 완료 ===");
        };
    }
}
