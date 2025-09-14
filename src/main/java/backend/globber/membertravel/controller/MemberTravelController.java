package backend.globber.membertravel.controller;

import backend.globber.membertravel.controller.dto.request.CreateMemberTravelRequest;
import backend.globber.membertravel.controller.dto.response.MemberTravelResponse;
import backend.globber.membertravel.service.MemberTravelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member-travels")
@RequiredArgsConstructor
public class MemberTravelController {

  private final MemberTravelService memberTravelService;

  @PostMapping("/{memberId}")
  public ResponseEntity<MemberTravelResponse> createMemberTravel(
      @PathVariable Long memberId
      , @Valid @RequestBody CreateMemberTravelRequest request) {
    // ApiResponseDto 만드나..?
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(memberTravelService.saveTravelRecord(memberId, request));
  }

  @GetMapping("/{memberId}/globe")
  public ResponseEntity<MemberTravelResponse> getMemberTravelRecords(@PathVariable Long memberId) {
    return ResponseEntity.ok(memberTravelService.getMemberTravelRecords(memberId));
  }
}
