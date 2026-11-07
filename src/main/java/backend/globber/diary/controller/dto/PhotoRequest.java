package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.constant.PhotoTag;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;


public record PhotoRequest(

        @Schema(description = "기존 사진 ID. 있으면 기존 사진을 유지하고 메타데이터를 수정하며, 없으면 새 사진으로 추가합니다.")
        Long photoId, // 있으면 기존 사진 유지·수정, 없으면 새 사진 추가

        @NotBlank(message = "S3 Key는 필수입니다.")
        String photoCode,  // S3에 업로드된 key

        @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
        Double lat,      // 사진 위도

        @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
        Double lng,      // 사진 경도

        @NotNull(message = "너비는 필수입니다.")
        @Min(value = 1, message = "너비는 1 이상이어야 합니다")
        Long width,      // 사진 너비

        @NotNull(message = "높이는 필수입니다.")
        @Min(value = 1, message = "높이는 1 이상이어야 합니다")
        Long height,     // 사진 높이

        @NotNull(message = "사진이 찍힌 시각은 필수입니다.")
        @JsonFormat(pattern = "yyyyMM")
        @Schema(
                description = "사진이 찍힌 시각 (YearMonth). yyyyMM 형식으로 전달",
                example = "202510",
                type = "string"
        )
        YearMonth takenMonth,   // 사진이 찍힌 시각

        PhotoTag tag,        // 태그

        String placeName // 사진이 찍힌 장소 이름 - 사용자 추가시 사용 가능할수도...?
) {

}

