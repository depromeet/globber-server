package backend.globber.controller;

import backend.globber.dto.request.ChangeNameRequest;
import backend.globber.dto.request.ChangePWRequest;
import backend.globber.dto.request.MailCertRequest;
import backend.globber.dto.request.MemberRequest;
import backend.globber.dto.response.ApiResponse;
import backend.globber.dto.response.JwtTokenResponse;
import backend.globber.dto.response.MemberResponse;
import backend.globber.service.MemberService;
import backend.globber.service.TokenService;
import backend.globber.util.CookieProvider;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final CookieProvider cookieProvider;
    private final TokenService tokenService;

    @PostMapping("/register/member/local")
    @Operation(summary = "회원가입", description = "회원가입을 진행합니다.")
    public ResponseEntity<ApiResponse<String>> registerLocalMember(@RequestBody MemberRequest request) {
        memberService.saveLocalMember(request.name(), request.email(), passwordEncoder.encode(request.password()));
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    // 중복검사
    @GetMapping("/duplicate/email/{email}")
    @Operation(summary = "이메일 중복검사", description = "이메일 중복검사를 진행합니다.")
    public ResponseEntity<ApiResponse<Boolean>> checkMemberEmail(@PathVariable String email) {
        boolean result = memberService.checkMemberEmail(email);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    // 메일인증 전송요청
    @GetMapping("/send/mail/{email}")
    @Operation(summary = "메일인증 전송", description = "메일인증을 위한 이메일을 전송합니다.")
    public void sendCertMail(@PathVariable String email) {
        memberService.sendCertMail(email);
    }

    // 메일인증 확인
    @PostMapping("/check/mail")
    @Operation(summary = "메일인증 확인", description = "메일인증을 확인합니다.")
    public ResponseEntity<ApiResponse<Boolean>> checkCertMail(@RequestBody MailCertRequest request) {
        boolean result = memberService.checkCertMail(request.email(), request.uuid());
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/members")
    @Operation(summary = "모든 회원 조회", description = "모든 회원을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> searchAllMember() {
        List<MemberResponse> allMember = memberService.findAllMember();
        return ResponseEntity.ok(ApiResponse.success(allMember));
    }

    @GetMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다.")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String accessToken){
        // 레디스에 있는 RefreshToken 삭제
        tokenService.logout(accessToken);
        // 쿠키 삭제
        ResponseCookie responseCookie = cookieProvider.deleteRefreshCookie();

        // 헤더에 넣으며 쿠키 업데이트
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(ApiResponse.success("로그아웃 되었습니다."));

    }

    @GetMapping("/reissue")
    @Operation(summary = "AccessToken 재발급", description = "AccessToken을 재발급합니다.")
    public ResponseEntity<ApiResponse<?>> reissue(@RequestHeader("Authorization") String accessToken,
        @CookieValue("RefreshToken") String refreshToken){
        // RefreshToken으로 AccessToken 재발급
        JwtTokenResponse jwtTokenResponse = tokenService.updateAccessToken(accessToken, refreshToken);

        // 쿠키 업데이트
        ResponseCookie responseCookie = cookieProvider.createRefreshCookie(jwtTokenResponse.refreshToken());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(ApiResponse.success(jwtTokenResponse));
    }

    @PutMapping("/pwchange")
    @Operation(summary = "비밀번호 변경", description = "비밀번호를 변경.")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestHeader("Authorization") String accessToken,
        @RequestBody ChangePWRequest request){
        memberService.changePassword(accessToken, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

    @PutMapping("/namechange")
    @Operation(summary = "이름 변경", description = "이름을 변경.")
    public ResponseEntity<ApiResponse<String>> changeName(@RequestHeader("Authorization") String accessToken,
        @RequestBody ChangeNameRequest request) throws JsonProcessingException {
        memberService.changeName(accessToken, request.newName());
        return ResponseEntity.ok(ApiResponse.success("이름이 변경되었습니다."));
    }


    @GetMapping("/mypage")
    @Operation(summary = "회원정보 조회", description = "회원정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> searchMember(@RequestHeader("Authorization") String accessToken){
        MemberResponse memberResponse = memberService.findMember(accessToken);
        return ResponseEntity.ok(ApiResponse.success(memberResponse));
    }

    @GetMapping("/auth/kakao" )
    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 진행합니다.")
    public void kakaoLogin() {
    }
}