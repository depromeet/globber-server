package backend.globber.bookmark.service.constant;

import backend.globber.exception.spec.BookmarkException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookmarkSortType {
    LATEST("latest", "최신순"),
    NAME("name", "이름순");

    private final String value;
    private final String description;

    @JsonCreator
    public static BookmarkSortType fromValue(String value) {
        return Arrays.stream(values())
            .filter(type -> type.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new BookmarkException(
                "지원하지 않는 정렬 방식입니다. (latest, name 중 선택)"));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
