package backend.globber.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application-{profile}.yml의 app.test-accounts 설정을 읽어오는 클래스 - application-local.yml: 로컬 환경 테스트 계정 -
 * application-dev.yml: 개발 환경 테스트 계정 - application-prod.yml: 설정 없음 (프로덕션에서는 사용 안 함)
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class TestAccountProperties {

    private List<Account> testAccounts;

    public List<Account> getAccounts() {
        return testAccounts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Account {

        private String email;
        private String nickname;
    }
}
