package backend.globber.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 현재 환경의 버킷 주소(aws.s3.base-url)로 S3 키를 전체 URL로 바꾼다.
 */
@Component
public class S3UrlResolver {

    private final String baseUrl;

    public S3UrlResolver(@Value("${aws.s3.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String toUrl(String key) {
        return S3Urls.join(baseUrl, key);
    }
}
