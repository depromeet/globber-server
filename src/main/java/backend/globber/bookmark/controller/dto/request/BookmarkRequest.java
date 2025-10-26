package backend.globber.bookmark.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record BookmarkRequest(
    @NotNull(message = "북마크 대상 회원 ID는 필수입니다.")
    @Positive(message = "회원 ID는 양수여야 합니다.")
    Long targetMemberId
) {

}
