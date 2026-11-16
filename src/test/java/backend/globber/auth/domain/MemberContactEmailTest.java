package backend.globber.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberContactEmailTest {

    private Member member(String email) {
        return Member.of(email, "테스터", null, AuthProvider.KAKAO, List.of(Role.ROLE_USER), "abc123");
    }

    @Test
    @DisplayName("'숫자@kakao' 형식의 로그인 식별자는 연락용 이메일이 아니므로 null이다")
    void syntheticKakaoId_isNotContactEmail() {
        assertThat(member("4464059339@kakao").getContactEmail()).isNull();
    }

    @Test
    @DisplayName("실제 이메일과, 숫자가 아닌 @kakao 값은 그대로 돌려준다")
    void otherEmails_kept() {
        assertThat(member("user@example.com").getContactEmail()).isEqualTo("user@example.com");
        assertThat(member("user@kakao.com").getContactEmail()).isEqualTo("user@kakao.com");
        assertThat(member("testuser1@kakao").getContactEmail()).isEqualTo("testuser1@kakao");
    }
}
