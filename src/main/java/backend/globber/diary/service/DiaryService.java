package backend.globber.diary.service;

import backend.globber.diary.controller.dto.DiaryListResponse;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;

public interface DiaryService {

    DiaryResponse createDiaryWithPhoto(Long memberId, DiaryRequest diaryRequest);

    DiaryResponse updateDiary(Long memberId, Long diaryID, DiaryRequest diaryRequest);

    void deleteDiary(Long memberId, Long diaryId);

    DiaryResponse getDiaryDetail(Long memberId, Long diaryId);

    DiaryResponse getDiaryDetail(Long diaryId);

    DiaryListResponse getDiariesByUUID(String UUID);
}
