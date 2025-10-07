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
    PhotoTag tag) {

}
