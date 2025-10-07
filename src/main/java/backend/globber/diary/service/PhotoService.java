package backend.globber.diary.service;

import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import java.util.List;

public interface PhotoService {

    PhotoResponse savePhoto(Long diaryId, PhotoRequest photoRequest);

    List<PhotoResponse> getAllPhoto(Long diaryId);

    PhotoResponse getPhotoId(Long photoId);

    PhotoResponse updatePhoto(Long diaryId, PhotoRequest photoRequest);

    void deletePhoto(Long diaryId, Long photoId);


}
