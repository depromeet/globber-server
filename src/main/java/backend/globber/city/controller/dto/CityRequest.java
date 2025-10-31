package backend.globber.city.controller.dto;

import backend.globber.city.domain.City;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CityRequest(
    @NotNull(message = "도시명은 필수입니다")
    String cityName,

    @NotNull(message = "국가명은 필수입니다")
    String countryName,

    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    Double lat,

    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    Double lng,

    @NotNull(message = "국가 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 3166-1 Alpha-3 형식이어야 합니다")
    String countryCode
) {
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