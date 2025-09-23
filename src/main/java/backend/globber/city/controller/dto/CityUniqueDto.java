package backend.globber.city.controller.dto;

import lombok.Builder;

@Builder
public record CityUniqueDto(
    String countryCode,
    String cityName,
    Double lat,
    Double lng

) {
    public CityUniqueDto of(String countryCode, String cityName, Double lat, Double lng) {
        return CityUniqueDto.builder()
            .countryCode(countryCode)
            .cityName(cityName)
            .lat(lat)
            .lng(lng)
            .build();
    }
}
