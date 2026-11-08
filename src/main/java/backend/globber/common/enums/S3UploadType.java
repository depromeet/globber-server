package backend.globber.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * S3 업로드 경로. 모두 토큰의 회원 ID를 기준으로 만들어, 키만 보고 누구의 파일인지 알 수 있게 한다.
 */
@Getter
@RequiredArgsConstructor
public enum S3UploadType {
    PROFILE("profiles"),
    TRAVEL("travels"),
    // 현재 클라이언트에서 쓰지 않지만, resourceId를 없애면서 다른 타입과 같은 회원 기준 경로로 맞춘다
    THUMBNAIL("thumbnails");

    private final String directory;

    public String generatePrefix(Long memberId) {
        return directory + "/" + memberId;
    }
}
