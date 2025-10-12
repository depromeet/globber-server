package backend.globber.diary.controller.dto;

import backend.globber.city.domain.City;
import java.util.List;

public record DiaryResponse(
    Long diaryId,         // 일기 ID
    City city,         // 도시 ID
    String text,      // 기록 내용
    String emoji,        // 이모지
    String createdAt,    // 생성 날짜
    String updatedAt,    // 수정 날짜
    List<PhotoResponse> photos // 첨부된 사진 정보 리스트
) {

}
