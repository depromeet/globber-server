package backend.globber.membertravel.controller.dto;

import java.util.List;

public record RegionDto(
        String regionName,
        int cityCount,
        List<CityDto> cities
) {
}