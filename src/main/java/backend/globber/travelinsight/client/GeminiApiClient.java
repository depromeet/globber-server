package backend.globber.travelinsight.client;

import backend.globber.exception.spec.GeminiException;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.controller.dto.response.MemberTravelResponse;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiApiClient(@Value("${gemini.api.url}") String apiUrl,
        @Value("${gemini.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
            .baseUrl(apiUrl)
            .build();
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    public TravelInsightResponse createTitle(MemberTravelAllResponse travels) {
        String prompt = createPrompt(travels);

        Map<String, Object> requestBody = createRequestBody(prompt);

        try {
            String response = restClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("GeminiApiClientAI.createTitle() 인사이트 생성 실패: {}", e.getMessage(), e);
            throw new GeminiException();
//            return TravelInsightResponse.empty(); 상위로 전달해서 empty() 생성
        }
    }

    private static Map<String, Object> getTemperature() {
        return Map.of(
            "temperature", 0.8
            , "maxOutputTokens", 25
        );
    }

    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> generationConfig = getTemperature();

        return Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of(
                    "text", prompt
                ))
            )),
            "generationConfig", generationConfig
        );
    }

    private String createPrompt(MemberTravelAllResponse travels) {
        StringBuilder memberTravels = new StringBuilder();
        List<MemberTravelResponse> responses = travels.travels();
        for (MemberTravelResponse response : responses) {
            response.cities().forEach(cityDto -> memberTravels.append(cityDto.countryName())
                .append(" ")
                .append(cityDto.cityName())
                .append("\n"));
        }

        return String.format(
            """
                여행지: %s
                
                위 여행지를 바탕으로 '~한 탐험가' 형태의 타이틀을 정확히 1개만 생성하세요.
                조건:
                - 10글자 이내
                - 설명 없이 타이틀만
                - 한국어
                - 예시: '문화유산 탐험가', '아시아 탐험가'
                
                타이틀:""",
            memberTravels
        );
    }

    private TravelInsightResponse parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return TravelInsightResponse.builder()
                .title(jsonNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText("자유로운 여행자")) // default value
                .build();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage(), e);
            throw new GeminiException();
        }
    }
}
