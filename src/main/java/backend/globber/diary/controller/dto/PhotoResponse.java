package backend.globber.diary.controller.dto;

import backend.globber.diary.domain.constant.PhotoTag;
import java.time.YearMonth;

public record PhotoResponse(
    Long photoId,       // 사진 ID
    String photoCode,    // 화면 표시용 키. 큰 WebP가 있으면 사용하고, 없으면 기존 업로드 키를 반환한다.
    Double lat,
    Double lng,
    Long width,
    Long height,
    YearMonth takenMonth,
    String placeName,
    PhotoTag tag,
    String thumbnailCode) {

}
