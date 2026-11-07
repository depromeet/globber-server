package backend.globber.diary.domain;

import backend.globber.exception.spec.InvalidEmojiCodeException;
import java.util.regex.Pattern;

/**
 * 이모지 code(소문자 16진 유니코드 코드포인트, 여러 개면 '-'로 연결. 예: 1f607, 2764-fe0f)를 검사하고
 * 실제 이모지 글자(glyph)로 바꾼다. glyph는 클라이언트에게 받지 않고 서버가 만든다.
 */
public final class EmojiCode {

    private static final Pattern FORMAT = Pattern.compile("^[0-9a-f]{1,6}(-[0-9a-f]{1,6})*$");
    // diary_emoji.glyph 컬럼 길이(varchar(8))
    private static final int MAX_GLYPH_LENGTH = 8;

    private EmojiCode() {
    }

    public static String toGlyph(String code) {
        if (code == null || !FORMAT.matcher(code).matches()) {
            throw new InvalidEmojiCodeException();
        }
        StringBuilder glyph = new StringBuilder();
        for (String part : code.split("-")) {
            int codePoint = Integer.parseInt(part, 16);
            if (!Character.isValidCodePoint(codePoint)
                || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) {
                throw new InvalidEmojiCodeException();
            }
            glyph.appendCodePoint(codePoint);
        }
        if (glyph.length() > MAX_GLYPH_LENGTH) {
            throw new InvalidEmojiCodeException();
        }
        return glyph.toString();
    }
}
