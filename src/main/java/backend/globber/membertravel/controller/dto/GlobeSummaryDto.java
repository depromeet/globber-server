package backend.globber.membertravel.controller.dto;

import java.util.List;

public record GlobeSummaryDto(
        int cityCount,
        int countryCount,
        List<RegionDto> regions
) {
}
