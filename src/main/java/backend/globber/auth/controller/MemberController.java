package backend.globber.auth.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.auth.dto.response.JwtTokenResponse;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final CookieProvider cookieProvider;
    private final TokenService tokenService;


    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다.")
    public ResponseEntity<ApiResponse<String>> logout(
        @RequestHeader("Authorization") String accessToken) {
        // 레디스에 있는 RefreshToken 삭제
        tokenService.logout(accessToken);
        // 쿠키 삭제
        ResponseCookie responseCookie = cookieProvider.deleteRefreshCookie();

        // 헤더에 넣으며 쿠키 업데이트
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(ApiResponse.success("로그아웃 되었습니다."));
    }

    @PostMapping("/reissue")
    @Operation(summary = "AccessToken 재발급", description = "AccessToken을 재발급합니다.")
    public ResponseEntity<ApiResponse<?>> reissue(
        @RequestHeader("Authorization") String accessToken,
        @CookieValue("RefreshToken") String refreshToken) {
        // RefreshToken으로 AccessToken 재발급
        JwtTokenResponse jwtTokenResponse = tokenService.updateAccessToken(accessToken,
            refreshToken);

        // 쿠키 업데이트
        ResponseCookie responseCookie = cookieProvider.createRefreshCookie(
            jwtTokenResponse.refreshToken());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(ApiResponse.success(jwtTokenResponse));
    }

    @GetMapping("/id")
    @Operation(summary = "멤버아이디 리턴", description = "[테스트용] 토큰을 기반으로 멤버아이디를 리턴받습니다.")
    public ResponseEntity<ApiResponse<Long>> getMemberId(
        @RequestHeader("Authorization") String accessToken) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        return ResponseEntity.ok(ApiResponse.success(memberId));
    }

}