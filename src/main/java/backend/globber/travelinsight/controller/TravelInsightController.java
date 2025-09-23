package backend.globber.travelinsight.controller;

import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.service.TravelInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TravelInsightController {

  private final TravelInsightService travelInsightService;

  @RequestMapping("/{memberId}/travel-insight")
  public ResponseEntity<TravelInsightResponse> getMemberTravelInsight(@PathVariable("memberId") Long memberId) {
    TravelInsightResponse response = travelInsightService.getOrCreateInsight(memberId);
    return ResponseEntity.ok(response);
  }
}
