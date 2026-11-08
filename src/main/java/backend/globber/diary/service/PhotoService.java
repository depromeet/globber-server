package backend.globber.diary.service;

import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;

import java.util.List;

public interface PhotoService {

    // 기록의 사진을 요청 목록(최종 상태)과 일치시킨다. 배열 인덱스가 표시 순서가 된다.
    void syncPhotos(Diary diary, List<PhotoRequest> photoRequests);

    List<PhotoResponse> getAllPhoto(Long diaryId);

    PhotoResponse getPhotoId(Long photoId);

}
