package backend.globber.diary.controller.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record DiaryRequest(

        @Schema(description = "도시 ID. 기록 생성 시에만 사용하고, 수정 시에는 무시합니다(도시는 바뀌지 않음).")
        @NotNull(message = "cityId는 필수입니다.")
        Long cityId,         // 도시 선택 ID -> 그냥 도시 아이디

        @Size(max = 200, message = "기록 내용은 최대 200자까지 작성할 수 있습니다.")
        String text,      // 기록 내용

        @Valid
        @NotNull(message = "첨부 사진은 필수입니다.")
        @Size(max = 3, message = "첨부 사진은 최대 3장까지 가능합니다.")
        @Schema(description = "최종 사진 목록(최대 3장). 배열 순서(0부터)가 사진 표시 순서로 저장되며, 첫 번째 사진이 대표 사진입니다.")
        List<PhotoRequest> photos // 첨부된 사진 정보 리스트
) {

}