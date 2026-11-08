package backend.globber.auth.controller;

import backend.globber.auth.dto.request.LoginCodeRequest;
import backend.globber.auth.dto.response.JwtTokenResponse;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import backend.globber.auth.util.LoginCodeStore;
import backend.globber.auth.util.LoginCodeStore.LoginResult;
import backend.globber.exception.spec.CustomAuthException;
import jakarta.validation.Valid;
import backend.globber.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "로그인, 로그아웃, 토큰 재발급 등 인증 관련 API")
public class MemberController {

    private final CookieProvider cookieProvider;
    private final TokenService tokenService;
    private final LoginCodeStore loginCodeStore;

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

    @PostMapping("/auth/token")
    @Operation(summary = "로그인 결과 받기", description = "로그인 성공 리다이렉트로 받은 일회용 code를 교환해 "
        + "accessToken(\"Bearer \" 접두어 포함), memberId, uuid, firstLogin을 받습니다. "
        + "code는 60초 동안 한 번만 쓸 수 있고, 잘못되었거나 만료되었거나 이미 쓴 code면 401입니다.")
    public ResponseEntity<ApiResponse<LoginResult>> exchangeLoginCode(@Valid @RequestBody LoginCodeRequest request) {
        LoginResult result = loginCodeStore.consume(request.code())
            .orElseThrow(() -> new CustomAuthException("유효하지 않거나 만료된 로그인 코드입니다."));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원탈퇴", description = "회원 정보를 삭제하고 로그아웃 처리합니다.")
    public ResponseEntity<ApiResponse<String>> withdraw(
        @RequestHeader("Authorization") String accessToken) {

        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);

        tokenService.deleteMember(memberId);

        tokenService.logout(accessToken);
        ResponseCookie responseCookie = cookieProvider.deleteRefreshCookie();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(ApiResponse.success("회원탈퇴가 완료되었습니다."));
    }
}
