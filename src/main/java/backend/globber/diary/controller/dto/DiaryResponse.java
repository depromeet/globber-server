package backend.globber.diary.controller.dto;

import backend.globber.city.controller.dto.CityResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DiaryResponse(
        Long diaryId,         // 일기 ID
        CityResponse city,         // 도시 ID
        String text,      // 기록 내용
        @Schema(description = "생성 시각. UTC 오프셋(Z)이 붙은 ISO 8601", example = "2026-01-31T11:18:38.747633Z", format = "date-time")
        String createdAt,    // 생성 날짜
        @Schema(description = "수정 시각. UTC 오프셋(Z)이 붙은 ISO 8601", example = "2026-01-31T11:18:38.747633Z", format = "date-time")
        String updatedAt,    // 수정 날짜
        List<PhotoResponse> photos, // 첨부된 사진 정보 리스트
        List<EmojiResponse> emojis,
        Long memberId,
        String nickname,
        @Schema(description = "작성자 프로필 이미지 전체 URL. 없으면 null", example = "https://globber-prod.s3.ap-northeast-2.amazonaws.com/profiles/6/1763216103109-7195742e.jpeg")
        String profileImageUrl
) {

}