package backend.globber.diary.controller.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사진 촬영 월을 "yyyy-MM"(권장)과 기존 "yyyyMM" 두 형식으로 받는다(GLB-212).
 * 빈 문자열은 기존처럼 null로 둔다. 그 밖의 값은 역직렬화 오류(400)로 처리한다.
 */
public class TakenMonthDeserializer extends JsonDeserializer<YearMonth> {

    private static final Pattern FORMAT = Pattern.compile("^(\\d{4})-?(\\d{2})$");

    @Override
    public YearMonth deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = FORMAT.matcher(value.trim());
        if (matcher.matches()) {
            try {
                return YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            } catch (DateTimeException ignored) {
                // 없는 월(00, 13 등)은 아래 형식 오류로 처리한다
            }
        }
        return (YearMonth) context.handleWeirdStringValue(YearMonth.class, value,
            "takenMonth는 yyyy-MM 또는 yyyyMM 형식이어야 합니다.");
    }
}
