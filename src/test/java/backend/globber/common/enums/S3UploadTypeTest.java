package backend.globber.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3UploadTypeTest {

    @Test
    @DisplayName("업로드 경로는 모두 토큰의 회원 ID를 기준으로 만든다")
    void prefixUsesMemberId() {
        // when & then
        assertThat(S3UploadType.TRAVEL.generatePrefix(7L)).isEqualTo("travels/7");
        assertThat(S3UploadType.PROFILE.generatePrefix(7L)).isEqualTo("profiles/7");
        assertThat(S3UploadType.THUMBNAIL.generatePrefix(7L)).isEqualTo("thumbnails/7");
    }
}
