package backend.globber.diary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.globber.exception.spec.InvalidEmojiCodeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmojiCodeTest {

    @Test
    @DisplayName("코드포인트 하나를 이모지 글자로 바꾼다")
    void singleCodePoint() {
        // when & then
        assertThat(EmojiCode.toGlyph("1f607")).isEqualTo("😇");
    }

    @Test
    @DisplayName("-로 이은 여러 코드포인트를 하나의 이모지 글자로 바꾼다")
    void multipleCodePoints() {
        // when & then
        assertThat(EmojiCode.toGlyph("2764-fe0f")).isEqualTo("❤️");
        assertThat(EmojiCode.toGlyph("1f468-200d-1f469")).isEqualTo("👨‍👩");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "1F607",            // 대문자
        "smile",            // 16진수가 아님
        "1f607-",           // 끝이 -
        "-1f607",           // 앞이 -
        "1f607--1f608",     // 빈 코드포인트
        "1100000",          // 7자리
        "110000",           // 유니코드 범위 밖
        "d800",             // 서로게이트
        "1f607 ",           // 공백
        "1f468-200d-1f469-200d-1f467-200d-1f466" // glyph가 저장 길이(8자)를 넘음
    })
    @DisplayName("형식이 맞지 않거나 저장할 수 없는 코드는 400 예외를 던진다")
    void invalidCode(String code) {
        // when & then
        assertThatThrownBy(() -> EmojiCode.toGlyph(code))
            .isInstanceOf(InvalidEmojiCodeException.class);
    }
}
