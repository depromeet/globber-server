package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.constant.PhotoTag;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;

public record PhotoResponse(
    Long photoId,       // 사진 ID
    @Schema(description = "화면 표시용 S3 키(전체 URL 아님). 큰 WebP가 있으면 그 키, 없으면 업로드 키.", example = "travels/6/1763216103109-7195742e.jpg")
    String photoCode,
    @Schema(description = "화면 표시용 전체 URL(photoCode에 버킷 주소를 붙인 값)", example = "https://globber-prod.s3.ap-northeast-2.amazonaws.com/travels/6/1763216103109-7195742e.jpg")
    String photoUrl,
    Double lat,
    Double lng,
    Long width,
    Long height,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    @Schema(description = "사진이 찍힌 월 (yyyy-MM)", example = "2025-10", type = "string", pattern = "^\\d{4}-\\d{2}$")
    YearMonth takenMonth,
    String placeName,
    PhotoTag tag,
    @Schema(description = "썸네일 S3 키. 없으면 null", example = "thumbnails/6/1763216103109-7195742e.webp")
    String thumbnailCode,
    @Schema(description = "썸네일 전체 URL. thumbnailCode가 없으면 null", example = "https://globber-prod.s3.ap-northeast-2.amazonaws.com/thumbnails/6/1763216103109-7195742e.webp")
    String thumbnailUrl) {

}
