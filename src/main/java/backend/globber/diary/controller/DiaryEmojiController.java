package backend.globber.diary.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.service.DiaryEmojiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diaries/{diaryId}/emojis")
@RequiredArgsConstructor
@Tag(name = "이모지 API", description = "여행 기록의 이모지 등록 및 반응(리액션) 관련 API")
public class DiaryEmojiController {

    private final DiaryEmojiService emojiService;

    @Operation(
            summary = "이모지 등록",
            description = "특정 다이어리에 새로운 이모지를 등록합니다. 이미 존재하는 이모지는 중복 등록할 수 없으며, 등록 시 count 값은 0으로 초기화됩니다."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmojiResponse>> registerEmoji(
            @PathVariable Long diaryId,
            @RequestParam String code,
            @RequestParam String glyph
    ) {
        EmojiResponse emojiResponse = emojiService.registerEmoji(diaryId, code, glyph);
        return ResponseEntity.ok(ApiResponse.success(emojiResponse));
    }

    @Operation(
            summary = "이모지 리액션 증가",
            description = "이미 등록된 이모지의 리액션(누르기)을 수행하여 count 값을 1 증가시킵니다."
    )
    @PostMapping("/press")
    public ResponseEntity<ApiResponse<Void>> pressEmoji(
            @PathVariable Long diaryId,
            @RequestParam String code
    ) {
        emojiService.pressEmoji(diaryId, code);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "이모지 목록 조회",
            description = "특정 다이어리에 등록된 모든 이모지와 각 카운트를 조회합니다. count 내림차순, 등록 순서 오름차순으로 정렬되어 반환됩니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmojiResponse>>> getEmojis(@PathVariable Long diaryId) {
        List<EmojiResponse> emojis = emojiService.getEmojis(diaryId);
        return ResponseEntity.ok(ApiResponse.success(emojis));
    }
}
