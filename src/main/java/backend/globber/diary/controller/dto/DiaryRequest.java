package backend.globber.diary.controller.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record DiaryRequest(

        @NotNull(message = "cityId는 필수입니다.")
        Long cityId,         // 도시 선택 ID -> 그냥 도시 아이디

        @Size(max = 200, message = "기록 내용은 최대 200자까지 작성할 수 있습니다.")
        String text,      // 기록 내용

        @Valid
        @NotNull(message = "첨부 사진은 필수입니다.")
        @Size(max = 3, message = "첨부 사진은 최대 3장까지 가능합니다.")
        List<PhotoRequest> photos // 첨부된 사진 정보 리스트
) {

}
