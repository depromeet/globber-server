package backend.globber.bookmark.controller.dto.request;

import lombok.Builder;

@Builder
public record BookmarkRequest(
    Long targetMemberId
) {

}
