package backend.globber.common.util;

/**
 * S3 키를 화면에서 바로 쓸 수 있는 전체 URL로 바꾼다(GLB-211).
 * 응답 필드 중 이름이 ...Url인 것은 이 규칙으로 전체 URL을 내려주고, ...Code인 것은 키 그대로 둔다.
 */
public final class S3Urls {

    private S3Urls() {
    }

    /**
     * @param baseUrl 버킷 주소(aws.s3.base-url). 끝의 슬래시는 있어도 되고 없어도 된다.
     * @param key     S3 키. null·빈 값이면 null, 이미 http(s)로 시작하면 그대로 돌려준다.
     */
    public static String join(String baseUrl, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + path;
    }
}
