package backend.globber.diary.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import backend.globber.diary.controller.dto.DiaryListResponse;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Tag(name = "여행기록 API", description = "여행기록 생성, 수정, 삭제, 조회 API")
public class DiaryController {

    private final DiaryService diaryService;
    private final CommonService commonService;


    @PostMapping
    @Operation(summary = "여행기록 생성",
            description = "새로운 여행기록을 생성합니다. 사진은 최대 3장이며, photos 배열 순서(0부터)가 사진 표시 순서로 저장됩니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> createDiary(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody DiaryRequest diaryRequest
    ) {
        //  Diary 저장
        // 이 트랜잭션에서 다이어리 저장 & 사진메타데이터 저장까지 처리
        Long memberId = commonService.getMemberIdFromToken(accessToken);

        DiaryResponse diary = diaryService.createDiaryWithPhoto(memberId, diaryRequest);
        return ResponseEntity.ok(ApiResponse.success(diary));
    }


    @PutMapping("/{diary_id}")
    @Operation(summary = "여행기록 수정",
            description = "본문과 최종 사진 목록(최대 3장)을 한 번에 반영합니다. "
                    + "photoId가 있는 사진은 유지하고 메타데이터를 수정하며, photoId가 없는 사진은 새로 추가합니다. "
                    + "목록에 없는 기존 사진은 삭제됩니다. photos 배열 순서가 사진 표시 순서입니다. "
                    + "다른 기록의 photoId가 포함되면 404를 반환하고 아무것도 변경하지 않습니다. "
                    + "새 사진끼리 또는 유지하는 사진과 photoCode가 같으면 400, 다른 기록이 쓰는 photoCode면 409입니다. "
                    + "요청의 cityId는 무시하며, 수정으로 기록의 도시는 바뀌지 않습니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> updateDiary(
            @RequestHeader("Authorization") String accessToken,
            @PathVariable Long diary_id,
            @RequestBody DiaryRequest diaryRequest
    ) {
        // 본문과 최종 사진 목록을 한 트랜잭션에서 반영
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        DiaryResponse diary = diaryService.updateDiary(memberId, diary_id, diaryRequest);
        return ResponseEntity.ok(ApiResponse.success(diary));
    }


    @DeleteMapping("/{diary_id}")
    @Operation(summary = "여행기록 삭제", description = "기존 여행기록을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @RequestHeader("Authorization") String accessToken,
            @PathVariable Long diary_id
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        diaryService.deleteDiary(memberId, diary_id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{diary_id}")
    @Operation(summary = "여행기록 상세 조회", description = "특정 여행기록의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> getDiaryDetail(
            @PathVariable Long diary_id
    ) {
        DiaryResponse response = diaryService.getDiaryDetail(diary_id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "여행기록들 UUID로 조회", description = "특정 UUID의 여행기록을 모두 조회합니다.")
    public ResponseEntity<ApiResponse<DiaryListResponse>> getDiariesByUUID(
            @RequestParam String uuid
    ) {
        DiaryListResponse response = diaryService.getDiariesByUUID(uuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

