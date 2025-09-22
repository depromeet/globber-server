package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import lombok.Builder;

@Builder
public record CityResponse(Long cityId, String cityName, String countryName) {

    public static CityResponse toResponse(City city) {
        return CityResponse.builder()
                .cityId(city.getCityId())
                .cityName(city.getCityName())
                .countryName(city.getCountryName())
                .build();
    }
}
