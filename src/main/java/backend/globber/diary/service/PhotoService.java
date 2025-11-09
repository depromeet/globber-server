package backend.globber.diary.service;

import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;

import java.util.List;

public interface PhotoService {

    PhotoResponse savePhoto(Long memberId, Long diaryId, PhotoRequest photoRequest);

    List<PhotoResponse> getAllPhoto(Long diaryId);

    PhotoResponse getPhotoId(Long photoId);

    void updatePhoto(Long memberId, Long diaryId, List<PhotoRequest> photoRequests);

    void deletePhoto(Long memberId, Long diaryId, Long photoId);


}
