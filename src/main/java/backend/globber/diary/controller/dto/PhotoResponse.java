package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.constant.PhotoTag;
import java.time.YearMonth;

public record PhotoResponse(
    Long photoId,       // 사진 ID
    String photoCode,
    Double lat,
    Double lng,
    Long width,
    Long height,
    YearMonth takenMonth,
    String placeName,
    PhotoTag tag,
    Integer displayOrder) { // 사진 노출 순서

}
