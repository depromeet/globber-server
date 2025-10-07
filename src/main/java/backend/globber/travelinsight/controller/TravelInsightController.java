package backend.globber.travelinsight.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.service.TravelInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/travel-insights")
@RequiredArgsConstructor
@Tag(name = "여행 인사이트 API", description = "멤버의 여행 데이터 기반 AI 인사이트 조회 API")
public class TravelInsightController {

    private final TravelInsightService travelInsightService;

    @GetMapping("/{memberId}")
    @Operation(summary = "멤버 여행 AI 인사이트 조회", description = "특정 멤버의 여행 데이터 기반 AI 인사이트를 조회합니다.")
    public ResponseEntity<ApiResponse<TravelInsightResponse>> getMemberTravelInsight(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(travelInsightService.getOrCreateInsight(memberId)));
    }
}
