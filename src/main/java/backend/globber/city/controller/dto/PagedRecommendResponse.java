package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedRecommendResponse(
        List<CityResponse> cityResponseList,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PagedRecommendResponse fromPage(final Page<City> cityPage) {
        return new PagedRecommendResponse(
                cityPage.getContent().stream().map(CityResponse::toResponse).toList(),
                cityPage.getNumber(),
                cityPage.getSize(),
                cityPage.getTotalElements(),
                cityPage.getTotalPages()
        );
    }
}
