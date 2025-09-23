package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import lombok.Builder;

@Builder
public record SearchResponse(
        Long cityId,
        String cityName,
        String countryName) {

    public static City toEntity(final SearchResponse searchResponse) {
        return City.builder()
                .cityId(searchResponse.cityId)
                .cityName(searchResponse.cityName)
                .countryName(searchResponse.countryName)
                .build();
    }
}

