package backend.globber.membertravel.controller;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.common.dto.ApiResponse;
import backend.globber.membertravel.controller.dto.request.CreateMemberTravelRequest;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.service.MemberTravelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member-travels")
@RequiredArgsConstructor
@Tag(name = "멤버 여행 기록 API", description = "멤버의 여행 기록 생성, 조회, 삭제 API")
public class MemberTravelController {

    private final MemberTravelService memberTravelService;

    @PostMapping("/{memberId}")
    @Operation(summary = "멤버 여행 기록 생성", description = "특정 멤버의 여행 기록을 생성합니다.")
    public ResponseEntity<ApiResponse<MemberTravelAllResponse>> createMemberTravel(
        @PathVariable final Long memberId
        , @Valid @RequestBody final List<CreateMemberTravelRequest> request) {

        MemberTravelAllResponse rtn = memberTravelService.createMemberTravel(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(rtn));
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "멤버 여행 기록 삭제", description = "특정 멤버의 여행 기록을 삭제합니다.")
    public ResponseEntity<ApiResponse<?>> deleteMemberTravel(@PathVariable final Long memberId,
        @RequestBody final CityUniqueDto city) {

        Boolean rtn = memberTravelService.deleteTravelRecord(memberId, city);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "멤버 여행 기록 조회", description = "특정 멤버의 여행 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<MemberTravelAllResponse>> getMemberTravelRecords(@PathVariable final Long memberId) {

        MemberTravelAllResponse rtn = memberTravelService.retrieveMemberTravel(memberId);

        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.success(rtn));
    }
}
