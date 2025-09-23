package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendResponse(List<CityResponse> cityResponseList) {

    public static RecommendResponse toResponse(final List<City> city) {
        return RecommendResponse.builder()
                .cityResponseList(city.stream().map(CityResponse::toResponse).toList())
                .build();
    }
}
