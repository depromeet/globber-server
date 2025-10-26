package backend.globber.membertravel.controller.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CityRecordDto(
        Long id,
        String name,
        boolean hasDiary,                 // 일기 존재 여부
        List<String> thumbnailUrls        // 대표 사진 목록
) {
}