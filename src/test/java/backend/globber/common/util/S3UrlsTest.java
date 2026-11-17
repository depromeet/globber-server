package backend.globber.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3UrlsTest {

    private static final String BASE = "https://globber-prod.s3.ap-northeast-2.amazonaws.com";

    @Test
    @DisplayName("S3 키에 버킷 주소를 붙여 전체 URL을 만든다")
    void join_keyToFullUrl() {
        // when & then
        assertThat(S3Urls.join(BASE, "profiles/6/a.jpeg")).isEqualTo(BASE + "/profiles/6/a.jpeg");
    }

    @Test
    @DisplayName("키가 null이거나 비어 있으면 null이다")
    void join_nullOrBlank() {
        // when & then
        assertThat(S3Urls.join(BASE, null)).isNull();
        assertThat(S3Urls.join(BASE, "")).isNull();
        assertThat(S3Urls.join(BASE, "  ")).isNull();
    }

    @Test
    @DisplayName("이미 http(s)로 시작하는 값은 그대로 둔다")
    void join_alreadyFullUrl() {
        // given
        String full = BASE + "/profiles/6/a.jpeg";

        // when & then
        assertThat(S3Urls.join(BASE, full)).isEqualTo(full);
        assertThat(S3Urls.join(BASE, "http://example.com/a.jpg")).isEqualTo("http://example.com/a.jpg");
    }

    @Test
    @DisplayName("버킷 주소 끝과 키 앞의 슬래시가 겹치거나 빠져도 슬래시 하나로 잇는다")
    void join_slashHandling() {
        // when & then
        assertThat(S3Urls.join(BASE + "/", "/travels/1/a.jpg")).isEqualTo(BASE + "/travels/1/a.jpg");
        assertThat(S3Urls.join(BASE + "/", "travels/1/a.jpg")).isEqualTo(BASE + "/travels/1/a.jpg");
        assertThat(S3Urls.join(BASE, "/travels/1/a.jpg")).isEqualTo(BASE + "/travels/1/a.jpg");
    }
}
