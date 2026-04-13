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
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "local"})
@Tag(name = "테스트 API", description = "[개발용] 테스트 계정 로그인 API")
public class TestLoginController {

    private final TokenService tokenService;
    private final CookieProvider cookieProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final TestAccountProperties testAccountProperties;

    @PostMapping("/dev/test-login")
    @Operation(summary = "[개발용] 테스트 계정 로그인",
        description = "[dev/local 환경만] 설정된 테스트 계정으로 바로 로그인하여 JWT 토큰을 발급받습니다.")
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

    /**
     * 설정된 테스트 계정인지 검증하는 메소드
     *
     * @param email 검증할 이메일
     * @throws IllegalArgumentException 테스트 계정이 아닌 경우
     */
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
