package backend.globber.bookmark.controller.dto.response;

import lombok.Builder;

@Builder
public record BookmarkedFriendResponse(
    Long memberId,
    String nickname,
    String profileImageUrl,
    boolean bookmarked
) {

}

