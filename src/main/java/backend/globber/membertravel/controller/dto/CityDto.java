package backend.globber.membertravel.controller.dto;

public record CityDto(
        String name,
        Double lat,
        Double lng,
        String countryCode
) {
}