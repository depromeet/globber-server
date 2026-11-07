package backend.globber.city.controller;

import backend.globber.city.controller.dto.CityRequest;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.service.CityService;
import backend.globber.city.service.SearchService;
import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
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
    private final CommonService commonService;

    private static final Duration ALL_CITIES_MAX_AGE = Duration.ofDays(1);

    @GetMapping("/favorites")
    @Operation(summary = "인기 여행지 조회", description = "인기 여행지 목록을 조회합니다. 응답은 ApiResponse로 감싸며 data에 cityResponseList를 담습니다.")
    public ResponseEntity<ApiResponse<RecommendResponse>> getFavorites(@RequestParam(defaultValue = "20") @Max(500) final int limit) {
        return ResponseEntity.ok(ApiResponse.success(cityService.getTopCities(limit)));
    }

    @GetMapping
    @Operation(summary = "도시 검색", description = """
        키워드 기반으로 도시/국가를 검색합니다. 유사도 + 인기순으로 정렬하며 최대 20개를 반환합니다.
        검색어는 앞뒤 공백을 제거해 사용하고, 비어 있거나 누락되면 400을 반환합니다.
        응답은 ApiResponse로 감싸며 data에 cities를 담습니다.""")
    public ResponseEntity<ApiResponse<SearchResult>> search(
        @Parameter(description = "검색어(앞뒤 공백 제거 후 1자 이상)", required = true)
        @RequestParam(required = false) final String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(keyword)));
    }

    @GetMapping("/all")
    @Operation(summary = "전체 도시 조회", description = """
        등록된 모든 도시를 조회합니다. 페이지네이션 없이 전체 목록을 반환합니다.
        Cache-Control(public, max-age=86400)과 응답 본문 전체로 계산한 ETag를 응답하며, If-None-Match가 현재 ETag와 같으면 본문 없이 304를 반환합니다.""")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getAllCities() {
        ApiResponse<List<CityResponse>> body = ApiResponse.success(cityService.getAllCities());
        // ETag 입력(UTF-8 JSON)과 본문 바이트가 같도록 Accept의 charset과 무관하게 UTF-8 JSON으로 고정한다.
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .cacheControl(CacheControl.maxAge(ALL_CITIES_MAX_AGE).cachePublic())
            .eTag(cityService.generateETag(body))
            .body(body);
    }

    // 도시 추가/삭제/수정 프로그램

    @PostMapping()
    @Operation(summary = "도시 추가", description = "새로운 도시를 추가합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> addCity(
        @RequestHeader("Authorization") String accessToken,
        @Valid @RequestBody final CityRequest cityRequest
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.addCity(memberId ,cityRequest);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @PutMapping("/{cityId}")
    @Operation(summary = "도시 수정", description = "기존 도시 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> updateCity(@PathVariable final Long cityId,
        @RequestHeader(value = "Authorization", required = false) String accessToken,
        @Valid @RequestBody final CityRequest cityRequest
    ) {
        Long memberId = commonService.getAdminMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.updateCity(memberId, cityId, cityRequest);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @DeleteMapping("/{cityId}")
    @Operation(summary = "도시 삭제", description = "기존 도시를 삭제합니다.")
    public ResponseEntity<ApiResponse<CityResponse>> deleteCity(@PathVariable final Long cityId,
        @RequestHeader(value = "Authorization", required = false) String accessToken
    ) {
        Long memberId = commonService.getAdminMemberIdFromToken(accessToken);
        CityResponse cityResponse = cityService.deleteCity(memberId, cityId);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }

    @GetMapping("/findCity")
    @Operation(summary = "도시 찾기", description = "나라코드와 도시명으로 도시를 찾습니다.")
    public ResponseEntity<ApiResponse<CityResponse>> findCityByCountryAndCityName(
        @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 3166-1 Alpha-3 형식이어야 합니다") @RequestParam String countryCode,
        @NotNull(message = "도시명은 필수입니다") @RequestParam String cityName
    ) {
        CityResponse cityResponse = cityService.findCityByCountryAndCityName(countryCode, cityName);
        return ResponseEntity.ok(ApiResponse.success(cityResponse));
    }
}
