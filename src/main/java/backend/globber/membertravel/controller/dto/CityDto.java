package backend.globber.membertravel.controller.dto;

public record CityDto(
        Long cityId,
        String name,
        Double lat,
        Double lng,
        String countryCode
) {
}