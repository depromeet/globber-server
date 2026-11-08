package backend.globber.common.constant;

import java.util.Locale;
import java.util.Set;

/**
 * 이미지 업로드 정책. presign 발급과 업로드된 객체 검증이 같은 기준을 사용한다.
 */
public final class ImageUploadPolicy {

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/avif"
    );

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private ImageUploadPolicy() {
    }

    /**
     * presign 요청의 Content-Type이 허용 형식과 정확히 일치하는지 확인한다.
     */
    public static boolean isAllowedContentType(String contentType) {
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
    }

    /**
     * S3에 저장된 Content-Type을 정규화(공백·대소문자·파라미터 제거)한 뒤 허용 형식인지 확인한다.
     */
    public static boolean isAllowedStoredContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return ALLOWED_CONTENT_TYPES.contains(mediaType);
    }
}
