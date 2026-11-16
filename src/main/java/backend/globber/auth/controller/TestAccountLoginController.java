package backend.globber.auth.controller;

import backend.globber.auth.dto.request.TestLoginRequest;
import backend.globber.auth.dto.response.TestLoginResponse;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.common.dto.ApiResponse;
import backend.globber.config.TestAccountProperties;
import backend.globber.exception.spec.CustomAuthException;
import backend.globber.exception.spec.ForbiddenException;
import backend.globber.exception.spec.MemberNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드용 테스트 계정 로그인 (GLB-383). 운영에서도 쓸 수 있다.
 * - app.test-login.key가 비어 있으면 API가 없는 것처럼 404를 돌려준다(기본은 닫힘).
 * - X-Test-Login-Key 헤더가 키와 같아야 하고, app.test-accounts에 있는 계정만 로그인할 수 있다.
 * - 액세스 토큰만 본문으로 준다(리프레시 토큰·쿠키·Authorization 헤더 없음).
 */
@Hidden
@Slf4j
@RestController
public class TestAccountLoginController {

    static final String KEY_HEADER = "X-Test-Login-Key";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final TestAccountProperties testAccountProperties;
    private final byte[] testLoginKey;

    public TestAccountLoginController(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository,
        TestAccountProperties testAccountProperties, @Value("${app.test-login.key:}") String testLoginKey) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        this.testAccountProperties = testAccountProperties;
        this.testLoginKey = StringUtils.hasText(testLoginKey) ? testLoginKey.getBytes(StandardCharsets.UTF_8) : null;
    }

    @PostMapping("/auth/test-login")
    public ResponseEntity<ApiResponse<?>> login(
        @RequestHeader(value = KEY_HEADER, required = false) String key,
        @RequestBody(required = false) TestLoginRequest request) {
        if (testLoginKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("요청한 경로를 찾을 수 없습니다."));
        }
        String email = request == null ? null : request.email();
        if (key == null || !MessageDigest.isEqual(testLoginKey, key.getBytes(StandardCharsets.UTF_8))) {
            log.warn("테스트 로그인 실패: 키 불일치 (account={})", email);
            throw new CustomAuthException("테스트 로그인 키가 올바르지 않습니다.");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
        if (!isTestAccount(email)) {
            log.warn("테스트 로그인 실패: 테스트 계정이 아님 (account={})", email);
            throw new ForbiddenException("테스트 계정만 로그인할 수 있습니다.");
        }
        if (!memberRepository.existsByEmail(email)) {
            log.warn("테스트 로그인 실패: 회원 없음 (account={})", email);
            throw new MemberNotFoundException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(email, List.of("ROLE_USER"));
        log.info("테스트 로그인 성공 (account={})", email);
        return ResponseEntity.ok(ApiResponse.success(new TestLoginResponse(accessToken)));
    }

    private boolean isTestAccount(String email) {
        List<TestAccountProperties.Account> accounts = testAccountProperties.getAccounts();
        return accounts != null && accounts.stream().anyMatch(account -> email.equals(account.getEmail()));
    }
}
