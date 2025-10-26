package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryRecordDto;
import lombok.Builder;

import java.util.List;

@Builder
public record TravelRecordWithDiaryResponse(
        long totalCountriesCounts,
        long totalCitiesCounts,
        long totalDiariesCounts,
        List<CountryRecordDto> records
) {
}