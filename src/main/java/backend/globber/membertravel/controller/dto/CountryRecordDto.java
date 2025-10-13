package backend.globber.membertravel.controller.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CountryRecordDto(
        String countryName,
        String countryCode,
        String continent,
        int diaryCount,
        List<CityRecordDto> cities
) {
}
