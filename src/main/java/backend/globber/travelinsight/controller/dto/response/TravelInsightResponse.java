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
        .title("여행 초보자")
        .build();
  }
}
