package backend.globber.city.controller;

import backend.globber.city.controller.dto.PagedRecommendResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.service.CityService;
import backend.globber.city.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PageableAsQueryParam
    @Operation(summary = "인기 여행지 조회", description = "인기 여행지 목록을 조회합니다.")
    public ResponseEntity<PagedRecommendResponse> getFavorites(@Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(cityService.getTopCities(pageable));
    }

    @GetMapping
    @Operation(summary = "도시 검색", description = "키워드 기반으로 도시/국가를 검색합니다. 유사도 + 인기순 정렬")
    public SearchResult search(@RequestParam final String keyword) {
        return searchService.search(keyword);
    }
}
