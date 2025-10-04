package backend.globber.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient geminiRestClient(@Value("${gemini.api.url}") String apiUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 서버 연결 대기 시간
        factory.setReadTimeout(5000); // AI 응답 대기 시간

        return RestClient.builder()
            .baseUrl(apiUrl)
            .requestFactory(factory)
            .build();
    }
}
