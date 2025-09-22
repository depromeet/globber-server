package backend.globber.membertravel.controller.dto;

import backend.globber.membertravel.domain.MemberTravelCity;

public record TravelCityDto(
    Long cityId,
    String cityName,
    String countryName,
    String countryCode,
    Double lat,
    Double lng)
{
    public static TravelCityDto from(MemberTravelCity mtc) {
        var city = mtc.getCity();
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

