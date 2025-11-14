package backend.globber.travelinsight.client;

import backend.globber.exception.spec.GeminiException;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.controller.dto.response.MemberTravelResponse;
import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelStatistics;
import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@Primary
public class GeminiApiClient implements AiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiApiClient(
        @Qualifier("geminiRestClient") RestClient restClient,
        @Value("${gemini.api.key}") String apiKey,
        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public TravelInsightResponse createTitle(MemberTravelAllResponse travels, TravelStatistics statistics) {
        String prompt = createPrompt(travels, statistics);

        Map<String, Object> requestBody = createRequestBody(prompt);

        try {
            String response = restClient.post()
                .uri("")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("GeminiApiClientAI.createTitle() 인사이트 생성 실패: {}", e.getMessage(), e);
//            throw new GeminiException();
            return TravelInsightResponse.empty();
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

    private String createPrompt(MemberTravelAllResponse travels, TravelStatistics statistics) {
        TravelLevel level = TravelLevel.determineLevel(statistics.getCountryCount(), statistics.getCityCount());
        TravelScope scope = TravelScope.determineScope(statistics.getCountryCount(), statistics.getContinentCount());
        TravelType type = TravelType.determineType(level, statistics.getPhotoTagCounts());

        return String.format(
            """
                당신은 여행 기록을 기반으로 뱃지를 명명하는 어시스턴트입니다.
                제공된 통계와 규칙을 참고해 정확히 1개의 타이틀만 출력하세요.
                
                [여행 통계]
                - 방문 국가 수: %d
                - 방문 도시 수: %d
                - 방문 대륙 수: %d
                - 음식 사진 수: %d
                - 인물 사진 수: %d
                - 풍경 사진 수: %d
                
                [타이틀 규칙]
                1. 형식: [형용사] + [두번째 표현] + [세번째 표현]
                2. 아래 단어를 그대로 사용해 조합하세요.
                   - 형용사: "%s"
                   - 두번째 표현: "%s"
                   - 세번째 표현: "%s"
                3. 10글자 이내, 한국어, 마침표나 설명 없이 결과만 출력
                4. 예시: "대담한 세계 탐험가", "호기심 많은 대륙 미식가"
                
                [여행 도시 목록]
                %s
                
                타이틀:""",
            statistics.getCountryCount(),
            statistics.getCityCount(),
            statistics.getContinentCount(),
            statistics.getPhotoTagCounts().getOrDefault(PhotoTag.FOOD, 0L),
            statistics.getPhotoTagCounts().getOrDefault(PhotoTag.PEOPLE, 0L),
            statistics.getPhotoTagCounts().getOrDefault(PhotoTag.SCENERY, 0L),
            level.getAdjective(),
            scope.getScopeName(),
            type.getTypeName(),
            buildTravelList(travels.travels())
        );
    }

    private String buildTravelList(List<MemberTravelResponse> responses) {
        StringBuilder memberTravels = new StringBuilder();
        for (MemberTravelResponse response : responses) {
            response.cities().forEach(cityDto -> memberTravels.append(cityDto.countryName())
                .append(" ")
                .append(cityDto.cityName())
                .append("\n"));
        }
        return memberTravels.toString();
    }

    private TravelInsightResponse parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return TravelInsightResponse.builder()
                .title(jsonNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("자유로운 여행자")) // default value
                .build();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage(), e);
            throw new GeminiException();
        }
    }
}
