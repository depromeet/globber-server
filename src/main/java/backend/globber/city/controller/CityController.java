package backend.globber.city.controller;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.service.CityService;
import backend.globber.city.service.SearchService;
import backend.globber.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Tag(name = "도시 API", description = "도시 검색 및 인기 여행지 조회 API")
public class CityController {

    private final CityService cityService;
    private final SearchService searchService;

    @GetMapping("/favorites")
    @Operation(summary = "인기 여행지 조회", description = "인기 여행지 목록을 조회합니다.")
    public ResponseEntity<RecommendResponse> getFavorites(@RequestParam(defaultValue = "20") @Max(500) final int limit) {
        return ResponseEntity.ok(cityService.getTopCities(limit));
    }

    @GetMapping
    @Operation(summary = "도시 검색", description = "키워드 기반으로 도시/국가를 검색합니다. 유사도 + 인기순 정렬")
    public SearchResult search(@RequestParam final String keyword) {
        return searchService.search(keyword);
    }

    @GetMapping("/id")
    @Operation(summary = "도시 ID 검색 By 위경도, 나라코드, 도시이름 (CityUniqueDto) ", description = "위경도, 나라코드, 도시이름으로 도시ID를 검색합니다.")
    public ResponseEntity<ApiResponse<Long>> getCityIdByUnique(
        @RequestBody final CityUniqueDto cityUniqueDto
    ) {
        Long cityId = cityService.getCityIdByUnique(cityUniqueDto);
        return ResponseEntity.ok(ApiResponse.success(cityId));
    }
}
