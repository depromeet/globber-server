package backend.globber.travelinsight.controller.dto.response;

import lombok.Builder;

@Builder
public record TravelInsightResponse(
    String title
) {

    public static TravelInsightResponse empty() {
        return TravelInsightResponse.builder()
            .title("여행 초보자")
            .build();
    }
}
