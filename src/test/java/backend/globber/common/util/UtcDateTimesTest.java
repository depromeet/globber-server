package backend.globber.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UtcDateTimesTest {

    @Test
    @DisplayName("서버 시간대가 UTC면 저장된 시각을 그대로 Z를 붙인 ISO 8601로 내려준다")
    void format_utcServer() {
        // given
        LocalDateTime saved = LocalDateTime.of(2026, 1, 31, 20, 18, 38, 747_633_000);

        // when
        String formatted = UtcDateTimes.format(saved, ZoneOffset.UTC);

        // then
        assertThat(formatted).isEqualTo("2026-01-31T20:18:38.747633Z");
    }

    @Test
    @DisplayName("서버 시간대가 UTC가 아니면 그 시간대 기준 시각을 UTC로 바꿔 Z를 붙인다")
    void format_nonUtcServer() {
        // given: 한국 시간으로 저장된 2026-02-01 05:18:38
        LocalDateTime saved = LocalDateTime.of(2026, 2, 1, 5, 18, 38);

        // when
        String formatted = UtcDateTimes.format(saved, ZoneId.of("Asia/Seoul"));

        // then
        assertThat(formatted).isEqualTo("2026-01-31T20:18:38Z");
    }

    @Test
    @DisplayName("초가 0이어도 초를 생략하지 않고, null은 null로 둔다")
    void format_zeroSecondsAndNull() {
        // when & then
        assertThat(UtcDateTimes.format(LocalDateTime.of(2026, 1, 31, 20, 18), ZoneOffset.UTC))
            .isEqualTo("2026-01-31T20:18:00Z");
        assertThat(UtcDateTimes.format(null, ZoneOffset.UTC)).isNull();
    }
}
