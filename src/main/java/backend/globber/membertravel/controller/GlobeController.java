package backend.globber.membertravel.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.membertravel.controller.dto.GlobeSummaryDto;
import backend.globber.membertravel.service.GlobeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/globes")
public class GlobeController {

    private final GlobeService globeService;

    public GlobeController(GlobeService globeService) {
        this.globeService = globeService;
    }

    @GetMapping("/{uuid}")
    @Operation(
            summary = "지구본 조회 API",
            description = "사용자의 UUID를 기반으로 해당 사용자의 여행 기록(지구본 요약)을 조회합니다. UUID는 소셜 로그인 성공 시 서버에서 발급되어 프론트엔드로 전달됩니다."
    )
    public ResponseEntity<ApiResponse<GlobeSummaryDto>> getGlobe(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(globeService.getGlobe(uuid)));
    }
}
