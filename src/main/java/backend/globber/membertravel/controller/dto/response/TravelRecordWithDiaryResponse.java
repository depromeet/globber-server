package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryRecordDto;
import lombok.Builder;

import java.util.List;

@Builder
public record TravelRecordWithDiaryResponse(
        int totalCountriesCounts,
        int totalCitiesCounts,
        int totalDiariesCounts,
        List<CountryRecordDto> records
) {
}