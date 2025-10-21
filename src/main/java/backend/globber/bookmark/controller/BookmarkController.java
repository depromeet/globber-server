package backend.globber.bookmark.controller;

import backend.globber.auth.service.TokenService;
import backend.globber.bookmark.controller.dto.request.BookmarkRequest;
import backend.globber.bookmark.service.BookmarkService;
import backend.globber.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "북마크 API", description = "타인의 북마크 지구본 API")
public class BookmarkController {

    private final TokenService tokenService;
    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> addBookmark(
        @RequestBody BookmarkRequest request,
        @RequestHeader("Authorization") String accessToken
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        bookmarkService.addBookmark(memberId, request.targetMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
