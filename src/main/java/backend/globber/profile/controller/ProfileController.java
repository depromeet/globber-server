package backend.globber.profile.controller;

import backend.globber.auth.service.TokenService;
import backend.globber.common.dto.ApiResponse;
import backend.globber.profile.controller.dto.response.ProfileResponse;
import backend.globber.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final TokenService tokenService;
    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
        @RequestHeader("Authorization") String accessToken
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        ProfileResponse profile = profileService.getProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
