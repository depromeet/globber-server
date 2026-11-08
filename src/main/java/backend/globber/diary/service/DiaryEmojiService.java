package backend.globber.diary.service;

import backend.globber.diary.controller.dto.EmojiResponse;
import java.util.List;

public interface DiaryEmojiService {

    /**
     * 이모지를 등록하고 등록한 사람이 누른 것으로 처리한다. 이미 있는 code면 누른 상태로 만든다(이미 눌렀으면 변화 없음).
     */
    EmojiResponse registerEmoji(Long memberId, Long diaryId, String code);

    /**
     * 누르기 토글: 누르지 않았으면 +1, 눌렀으면 -1.
     */
    EmojiResponse pressEmoji(Long memberId, Long diaryId, String code);

    /**
     * @param viewerId 조회한 회원. 비로그인이면 null(pressedByMe는 모두 false)
     */
    List<EmojiResponse> getEmojis(Long viewerId, Long diaryId);
}
