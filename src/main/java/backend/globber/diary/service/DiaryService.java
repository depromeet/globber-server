package backend.globber.diary.service;

import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;

public interface DiaryService {

    DiaryResponse createDiaryWithPhoto(String accessToken, DiaryRequest diaryRequest);

    DiaryResponse updateDiary(String accessToken, Long diaryID, DiaryRequest diaryRequest);

    void deleteDiary(String accessToken, Long diaryId);

    DiaryResponse getDiaryDetail(String accessToken, Long diaryId);
}
