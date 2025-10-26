package backend.globber.diary.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.service.DiaryService;
import backend.globber.diary.service.PhotoService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Tag(name = "여행기록 API", description = "여행기록 생성, 수정, 삭제, 조회 API")
public class DiaryController {

    private final DiaryService diaryService;
    private final PhotoService photoService;
    private final CommonService commonService;


    @PostMapping
    @Operation(summary = "여행기록 생성", description = "새로운 여행기록을 생성합니다.")
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
    @Operation(summary = "여행기록 수정", description = "기존 여행기록을 수정합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> updateDiary(
        @RequestHeader("Authorization") String accessToken,
        @PathVariable Long diary_id,
        @RequestBody DiaryRequest diaryRequest
    ) {
        // 프론트에서 기존 + 수정 데이터 모두 전달됨 → 서비스에서 update 처리만
        Long memberId = commonService.getMemberIdFromToken(accessToken);

        DiaryResponse diary = diaryService.updateDiary(memberId, diary_id, diaryRequest);
        return ResponseEntity.ok(ApiResponse.success(diary));
    }


    @PostMapping("/photo/{diary_id}")
    @Operation(summary = "여행기록 사진 추가", description = "기존 여행기록에 사진을 추가합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> addPhotoToDiary(
        @RequestHeader("Authorization") String accessToken,
        @PathVariable Long diary_id,
        @RequestBody PhotoRequest photoRequest
    ) {
        // 사진 저장 로직 실행
        // 이 트랜잭션에서 사진추가 & 다이어리에서의 저장까지 처리
        Long memberId = commonService.getMemberIdFromToken(accessToken);

        photoService.savePhoto(memberId, diary_id, photoRequest);
        // 사진 추가된 Diary 정보 반환
        DiaryResponse diary = diaryService.getDiaryDetail(memberId, diary_id);

        return ResponseEntity.ok(ApiResponse.success(diary));
    }


    @PutMapping("/photo/{diary_id}")
    @Operation(summary = "여행기록 사진 수정", description = "기존 여행기록에서 사진을 수정합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> updatePhotoToDiary(
        @RequestHeader("Authorization") String accessToken,
        @PathVariable Long diary_id,
        @RequestBody PhotoRequest photoRequest
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        // 사진 수정 로직 실행
        photoService.updatePhoto(memberId, diary_id, photoRequest);
        // 사진 수정된 Diary 정보 반환
        DiaryResponse diary = diaryService.getDiaryDetail(memberId, diary_id);

        return ResponseEntity.ok(ApiResponse.success(diary));
    }


    @DeleteMapping("/photo/{diary_id}/{photo_id}")
    @Operation(summary = "여행기록 사진 삭제", description = "기존 여행기록에서 사진을 삭제합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> deletePhotoFromDiary(
        @RequestHeader("Authorization") String accessToken,
        @PathVariable Long diary_id,
        @PathVariable Long photo_id
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        // 사진 삭제 로직 실행
        // 이 트랜잭션에서 사진삭제 & 다이어리에서의 삭제까지 처리
        photoService.deletePhoto(memberId, diary_id, photo_id);

        // 사진 삭제된 Diary 정보 반환
        DiaryResponse diary = diaryService.getDiaryDetail(memberId, diary_id);
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
        @RequestHeader("Authorization") String accessToken,
        @PathVariable Long diary_id
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        DiaryResponse response = diaryService.getDiaryDetail(memberId, diary_id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

