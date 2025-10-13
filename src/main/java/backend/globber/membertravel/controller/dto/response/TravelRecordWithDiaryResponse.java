package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryRecordDto;
import lombok.Builder;

import java.util.List;

@Builder
public record TravelRecordWithDiaryResponse(
        int totalCountries,
        int totalCities,
        int totalDiaries,
        List<CountryRecordDto> records
) {
}