package backend.globber.diary.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoRequestTakenMonthTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PhotoRequest read(String takenMonth) throws Exception {
        return objectMapper.readValue(
            "{\"photoCode\":\"travels/1/a.jpg\",\"width\":1,\"height\":1,\"takenMonth\":\"" + takenMonth + "\"}",
            PhotoRequest.class);
    }

    @Test
    @DisplayName("takenMonth는 yyyy-MM과 기존 yyyyMM 형식을 모두 받는다")
    void takenMonth_acceptsBothFormats() throws Exception {
        // when & then
        assertThat(read("2025-04").takenMonth()).isEqualTo(YearMonth.of(2025, 4));
        assertThat(read("202504").takenMonth()).isEqualTo(YearMonth.of(2025, 4));
    }

    @Test
    @DisplayName("takenMonth가 null이거나 빈 문자열이면 지금처럼 null로 둔다")
    void takenMonth_nullOrEmpty() throws Exception {
        // when & then
        assertThat(read("").takenMonth()).isNull();
        assertThat(objectMapper.readValue(
            "{\"photoCode\":\"travels/1/a.jpg\",\"width\":1,\"height\":1,\"takenMonth\":null}",
            PhotoRequest.class).takenMonth()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-13", "202500", "25-04", "2025/04", "2025-4", "abc"})
    @DisplayName("형식이 맞지 않거나 없는 월이면 역직렬화에 실패한다")
    void takenMonth_rejectsInvalid(String takenMonth) {
        // when & then
        assertThatThrownBy(() -> read(takenMonth)).isInstanceOf(MismatchedInputException.class);
    }
}
