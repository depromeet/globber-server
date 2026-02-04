package backend.globber.auth.controller;

import backend.globber.auth.dto.response.JwtTokenResponse;
import backend.globber.auth.service.TokenService;
import backend.globber.auth.util.CookieProvider;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.common.dto.ApiResponse;
import backend.globber.config.TestAccountProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증 API", description = "로그인, 로그아웃, 토큰 재발급 등 인증 관련 API")
public class MemberController {

    private final CookieProvider cookieProvider;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TestAccountProperties testAccountProperties;

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

    @PostMapping("/dev/test-login")
    @Operation(summary = "[개발용] 테스트 계정 로그인",
        description = "[dev/local 환경만] 테스트 계정으로 바로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<ApiResponse<JwtTokenResponse>> testLogin(
        @RequestParam String email) {

        log.info("테스트 로그인 요청: {}", email);

        validateTestAccount(email);

        String refreshToken = jwtTokenProvider.createRefreshToken();
        tokenService.updateRefreshToken(email, refreshToken);

        List<String> roles = List.of("ROLE_USER");
        String accessToken = jwtTokenProvider.createAccessToken(email, roles);

        ResponseCookie responseCookie = cookieProvider.createRefreshCookie(refreshToken);

        JwtTokenResponse response = JwtTokenResponse.toResponse(accessToken, refreshToken);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .header("Authorization", accessToken)
            .body(ApiResponse.success(response));
    }

    private void validateTestAccount(String email) {
        if (testAccountProperties.getAccounts() == null
            || testAccountProperties.getAccounts().isEmpty()) {
            log.error("설정된 테스트 계정이 없습니다.");
            throw new IllegalArgumentException("테스트 계정이 설정되지 않았습니다.");
        }

        boolean isValidTestAccount = testAccountProperties.getAccounts().stream()
            .anyMatch(account -> account.getEmail().equals(email));

        if (!isValidTestAccount) {
            log.warn("허용되지 않은 테스트 계정: {}", email);
            throw new IllegalArgumentException("허용되지 않은 테스트 계정입니다: " + email);
        }

        log.debug("테스트 계정 검증 성공: {}", email);
    }

}
