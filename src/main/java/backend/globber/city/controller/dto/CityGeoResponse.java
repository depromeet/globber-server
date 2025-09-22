package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import lombok.Builder;

@Builder
public record CityGeoResponse (Long cityId, String cityName, String countryName, Double lat, Double lng) {

    public static CityGeoResponse toResponse(City city) {
        return CityGeoResponse.builder()
            .cityId(city.getCityId())
            .cityName(city.getCityName())
            .countryName(city.getCountryName())
            .lat(city.getLat())
            .lng(city.getLng())
            .build();
    }
}
