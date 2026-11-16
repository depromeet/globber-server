package backend.globber.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * DB의 LocalDateTime(서버 JVM 시간대로 저장된 값)을 UTC 오프셋(Z)이 붙은 ISO 8601 문자열로 바꾼다.
 * 응답에 시간대가 없으면 브라우저가 현지 시각으로 해석해 시각이 어긋난다(GLB-213).
 */
public final class UtcDateTimes {

    // 초가 0이어도 생략하지 않고, 소수 초는 있을 때만 붙인다 (예: 2026-01-31T20:18:00Z, 2026-01-31T20:18:38.747633Z)
    private static final DateTimeFormatter ISO_UTC = new DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
        .optionalStart().appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
        .appendLiteral('Z')
        .toFormatter();

    private UtcDateTimes() {
    }

    public static String format(LocalDateTime dateTime) {
        return format(dateTime, ZoneId.systemDefault());
    }

    public static String format(LocalDateTime dateTime, ZoneId serverZone) {
        if (dateTime == null) {
            return null;
        }
        LocalDateTime utc = dateTime.atZone(serverZone).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
        return ISO_UTC.format(utc);
    }
}
