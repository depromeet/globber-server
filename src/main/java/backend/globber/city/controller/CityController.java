package backend.globber.city.controller;

import backend.globber.city.controller.dto.CityRequest;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.service.CityService;
import backend.globber.city.service.SearchService;
import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final CityRepository cityRepository;
    private final CommonService commonService;

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

    // 도시 추가/삭제/수정 프로그램

    @PostMapping()
    @Operation(summary = "도시 추가", description = "새로운 도시를 추가합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> addCity(
        @RequestHeader("Authorization") String accessToken,
        @RequestBody final CityRequest cityRequest
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.addCity(memberId ,cityRequest);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @PutMapping("/{cityId}")
    @Operation(summary = "도시 수정", description = "기존 도시 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> updateCity(@PathVariable final Long cityId,
        @RequestHeader("Authorization") String accessToken,
        @RequestBody final CityRequest cityRequest
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.updateCity(memberId, cityId, cityRequest);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @DeleteMapping("/{cityId}")
    @Operation(summary = "도시 삭제", description = "기존 도시를 삭제합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> deleteCity(@PathVariable final Long cityId,
        @RequestHeader("Authorization") String accessToken
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.deleteCity(memberId, cityId);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @GetMapping("/findCity")
    @Operation(summary = "도시 찾기", description = "나라코드와 도시명으로 도시를 찾습니다.")
    public ResponseEntity<ApiResponse<CityResponse>> findCityByCountryAndCityName(
        @RequestParam String countryCode,
        @RequestParam String cityName
    ) {
        CityResponse cityResponse = cityService.findCityByCountryAndCityName(countryCode, cityName);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }
}
