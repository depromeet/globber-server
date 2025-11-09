package backend.globber.diary.controller.dto;

import backend.globber.city.controller.dto.CityResponse;

import java.util.List;

public record CityGroupedDiaryResponse(
        CityResponse city,
        List<DiaryResponse> diaries
) {
}
