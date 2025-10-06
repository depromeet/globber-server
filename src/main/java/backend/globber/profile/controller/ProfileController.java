package backend.globber.profile.controller;

import backend.globber.auth.service.TokenService;
import backend.globber.common.dto.ApiResponse;
import backend.globber.profile.controller.dto.request.UpdateProfileImageRequest;
import backend.globber.profile.controller.dto.request.UpdateProfileRequest;
import backend.globber.profile.controller.dto.response.ProfileResponse;
import backend.globber.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "프로필 API", description = "사용자 프로필 조회 및 수정 API")
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

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인 사용자의 정보(닉네임)를 수정합니다.")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
        @RequestHeader("Authorization") String accessToken,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        ProfileResponse profile = profileService.updateProfile(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping("/me/image")
    @Operation(summary = "프로필 이미지 수정", description = "현재 로그인한 사용자의 프로필 이미지를 수정합니다.(프론트에서 S3에 업로드 후 S3 key를 전달)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileImage(
        @RequestHeader("Authorization") String accessToken,
        @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        Long memberId = tokenService.getMemberIdFromAccessToken(accessToken);
        ProfileResponse response = profileService.updateProfileImage(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
