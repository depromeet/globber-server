package backend.globber.bookmark.controller;

import backend.globber.auth.service.TokenService;
import backend.globber.bookmark.controller.dto.request.BookmarkRequest;
import backend.globber.bookmark.controller.dto.response.BookmarkedFriendResponse;
import backend.globber.bookmark.service.BookmarkService;
import backend.globber.bookmark.service.constant.BookmarkSortType;
import backend.globber.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "북마크 API", description = "타인의 북마크 지구본 API")
public class BookmarkController {

    private final TokenService tokenService;
    private final BookmarkService bookmarkService;

    @Value("${app.oauth.redirect-domain}")
    private String oauthRedirectDomain;

    @PostMapping
    @Operation(summary = "북마크 추가", description = "특정 사용자를 북마크에 추가합니다. (비회원 자동처리 포함)")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @RequestBody @Valid BookmarkRequest request,
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            HttpServletResponse response
    ) {
        // 로그인 여부 판단
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            // 비회원 → targetMemberId 쿠키에 저장
            ResponseCookie cookie = ResponseCookie.from("pendingBookmarkId", request.targetMemberId().toString())
                    .httpOnly(true)
                    .path("/")
                    .sameSite("None")
                    .secure(true)
                    .maxAge(Duration.ofHours(1))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // 로그인 페이지로 리다이렉트
            String redirectUrl = oauthRedirectDomain + "/oauth2/authorization/kakao";

            return ResponseEntity.ok()  // 401
                    .header("X-Redirect-URL", redirectUrl)
                    .body(ApiResponse.success(null));
        }

        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        bookmarkService.addBookmark(memberId, request.targetMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "북마크 목록 조회", description = "내가 북마크한 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<BookmarkedFriendResponse>>> getBookmarks(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(defaultValue = "LATEST") BookmarkSortType sort
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(memberId,
                sort);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{targetMemberId}")
    @Operation(summary = "북마크 삭제", description = "특정 사용자를 북마크에서 제거합니다.")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            @PathVariable Long targetMemberId,
            @RequestHeader("Authorization") String accessToken
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        bookmarkService.removeBookmark(memberId, targetMemberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
