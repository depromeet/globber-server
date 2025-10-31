package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import lombok.Builder;

@Builder
public record CityRequest(String cityName, String countryName, Double lat, Double lng,
                           String countryCode) {
    public City toCity() {
        return City.builder()
                .cityName(cityName)
                .countryName(countryName)
                .lat(lat)
                .lng(lng)
                .countryCode(countryCode)
                .build();
    }

    public CityRequest of(String cityName, String countryName, Double lat, Double lng,
                              String countryCode) {
        return CityRequest.builder()
                .cityName(cityName)
                .countryName(countryName)
                .lat(lat)
                .lng(lng)
                .countryCode(countryCode)
                .build();
    }
}