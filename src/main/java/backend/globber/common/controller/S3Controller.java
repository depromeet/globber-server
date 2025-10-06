package backend.globber.common.controller;

import backend.globber.auth.service.TokenService;
import backend.globber.common.controller.dto.request.PresignedUrlRequest;
import backend.globber.common.dto.ApiResponse;
import backend.globber.common.dto.PresignedUrlResponse;
import backend.globber.common.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/s3")
@RequiredArgsConstructor
@Tag(name = "S3 API", description = "S3 관련 API")
public class S3Controller {

    private final S3Service s3Service;
    private final TokenService tokenService;

    @PostMapping("/upload-url")
    @Operation(
        summary = "파일 업로드 Presigned URL 발급",
        description = "S3에 파일을 업로드하기 위한 Presigned URL을 발급합니다.  prefix에 업로드할 경로를 지정하세요."
    )
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getUploadUrl(
        @RequestHeader("Authorization") String accessToken,
        @Valid @RequestBody PresignedUrlRequest request
    ) {
        long memberId = tokenService.getMemberIdFromAccessToken(accessToken);

        PresignedUrlResponse response = s3Service.generatePresignedUrl(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
