package backend.globber.travelinsight.controller.dto.response;

import lombok.Builder;

@Builder
public record TravelInsightResponse(
    String title
) {

    public static TravelInsightResponse of(String title) {
        return TravelInsightResponse.builder()
            .title(title)
            .build();
    }

    public static TravelInsightResponse empty() {
        return TravelInsightResponse.builder()
            .title("자유로운 여행자")
            .build();
    }
}
