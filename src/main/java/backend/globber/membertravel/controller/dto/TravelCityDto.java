package backend.globber.membertravel.controller.dto;

import backend.globber.membertravel.domain.MemberTravelCity;
import java.util.Objects;

public record TravelCityDto(
    Long cityId,
    String cityName,
    String countryName,
    String countryCode,
    Double lat,
    Double lng)
{
    public static TravelCityDto from(MemberTravelCity mtc) {
        Objects.requireNonNull(mtc, "mtc must not be null");
        var city = Objects.requireNonNull(mtc.getCity(), "mtc.city must not be null");
        return new TravelCityDto(
            city.getCityId(),
            city.getCityName(),
            city.getCountryName(),
            city.getCountryCode(),
            city.getLat(),
            city.getLng()
        );
    }
}

