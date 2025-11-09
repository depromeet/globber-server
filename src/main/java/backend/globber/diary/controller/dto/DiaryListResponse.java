package backend.globber.diary.controller.dto;

import java.util.List;

public record DiaryListResponse(
        List<CityGroupedDiaryResponse> diaryResponses
) {
}
