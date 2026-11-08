package backend.globber.diary.service;

import backend.globber.diary.controller.dto.DiaryListResponse;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;

public interface DiaryService {

    DiaryResponse createDiaryWithPhoto(Long memberId, DiaryRequest diaryRequest);

    DiaryResponse updateDiary(Long memberId, Long diaryID, DiaryRequest diaryRequest);

    void deleteDiary(Long memberId, Long diaryId);

    DiaryResponse getDiaryDetail(Long memberId, Long diaryId);

    // viewerId: 조회한 회원(비로그인이면 null). 이모지의 pressedByMe 계산에 쓴다
    DiaryResponse getPublicDiaryDetail(Long diaryId, Long viewerId);

    DiaryListResponse getDiariesByUUID(String UUID, Long viewerId);
}
