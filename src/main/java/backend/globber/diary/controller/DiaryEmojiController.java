package backend.globber.diary.controller;

import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import backend.globber.diary.controller.dto.EmojiRequest;
import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.service.DiaryEmojiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries/{diaryId}/emojis")
@RequiredArgsConstructor
@Tag(name = "이모지 API", description = "여행 기록의 이모지 등록 및 반응(리액션) 관련 API")
public class DiaryEmojiController {

    private final DiaryEmojiService emojiService;
    private final CommonService commonService;

    @Operation(
            summary = "이모지 등록",
            description = "기록에 이모지를 등록하고, 등록한 사람이 누른 것으로 처리합니다(count 1). "
                    + "이미 있는 code면 오류 없이 누른 상태가 되고, 이미 눌렀으면 변화가 없습니다. "
                    + "glyph는 code로 서버가 만듭니다. 로그인이 필요합니다."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmojiResponse>> registerEmoji(
            @PathVariable Long diaryId,
            @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody EmojiRequest request
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        return ResponseEntity.ok(ApiResponse.success(emojiService.registerEmoji(memberId, diaryId, request.code())));
    }

    @Operation(
            summary = "이모지 누르기(토글)",
            description = "누르지 않은 이모지면 count를 1 올리고, 이미 누른 이모지면 취소하고 1 내립니다. "
                    + "한 사람은 이모지마다 한 번만 반응할 수 있습니다. 로그인이 필요합니다."
    )
    @PostMapping("/press")
    public ResponseEntity<ApiResponse<EmojiResponse>> pressEmoji(
            @PathVariable Long diaryId,
            @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody EmojiRequest request
    ) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        return ResponseEntity.ok(ApiResponse.success(emojiService.pressEmoji(memberId, diaryId, request.code())));
    }

    @Operation(
            summary = "이모지 목록 조회",
            description = "기록에 등록된 이모지와 count를 조회합니다. count 내림차순, 등록 순서 오름차순입니다. "
                    + "로그인한 경우 Authorization을 보내면 pressedByMe에 내가 눌렀는지가 표시되고, 비로그인이면 false입니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmojiResponse>>> getEmojis(
            @PathVariable Long diaryId,
            @RequestHeader(value = "Authorization", required = false) String accessToken
    ) {
        Long viewerId = accessToken == null ? null : commonService.getMemberIdFromToken(accessToken);
        return ResponseEntity.ok(ApiResponse.success(emojiService.getEmojis(viewerId, diaryId)));
    }
}
