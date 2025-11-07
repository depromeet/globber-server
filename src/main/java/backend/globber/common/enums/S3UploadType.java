package backend.globber.common.enums;

import backend.globber.exception.spec.InvalidUploadTypeException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public enum S3UploadType {
    PROFILE(false, (memberId, resourceId) -> "profiles/" + memberId),
    TRAVEL(true, (memberId, resourceId) -> "travels/" + resourceId),
    THUMBNAIL(true, (memberId, resourceId) -> "thumbnails/" + resourceId);

    private final boolean requiresResourceId;

    private final BiFunction<Long, Long, String> pathGenerator;

    public String generatePrefix(Long memberId, Long resourceId) {
        if (requiresResourceId && resourceId == null) {
            throw new InvalidUploadTypeException(this.name() + " 타입은 resourceId가 필수입니다.");
        }
        return pathGenerator.apply(memberId, resourceId);
    }
}
