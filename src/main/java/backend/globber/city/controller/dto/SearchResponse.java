package backend.globber.city.controller.dto;

public record SearchResponse(
        Long cityId,
        String cityName,
        String countryName) {
}

