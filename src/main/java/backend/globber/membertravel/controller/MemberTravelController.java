package backend.globber.membertravel.controller;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.common.dto.ApiResponse;
import backend.globber.common.service.CommonService;
import backend.globber.membertravel.controller.dto.request.CreateMemberTravelRequest;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.controller.dto.response.TravelRecordWithDiaryResponse;
import backend.globber.membertravel.service.MemberTravelService;
import backend.globber.membertravel.service.TravelRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/member-travels")
@RequiredArgsConstructor
@Tag(name = "멤버 여행 기록 API", description = "멤버의 여행 기록 생성, 조회, 삭제 API")
public class MemberTravelController {

    private final MemberTravelService memberTravelService;
    private final CommonService commonService;
    private final TravelRecordQueryService travelRecordQueryService;

    @PostMapping()
    @Operation(summary = "멤버 여행 기록 생성", description = "특정 멤버의 여행 기록을 생성합니다.")
    public ResponseEntity<ApiResponse<MemberTravelAllResponse>> createMemberTravel(
            @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody final List<CreateMemberTravelRequest> request) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        MemberTravelAllResponse rtn = memberTravelService.createMemberTravel(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rtn));
    }

    @DeleteMapping()
    @Operation(summary = "멤버 여행 기록 삭제", description = "특정 멤버의 여행 기록을 삭제합니다.")
    public ResponseEntity<ApiResponse<?>> deleteMemberTravel(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody final CityUniqueDto city) {

        Long memberId = commonService.getMemberIdFromToken(accessToken);
        Boolean rtn = memberTravelService.deleteTravelRecord(memberId, city);

        return ResponseEntity.noContent().build();
    }

    @GetMapping()
    @Operation(summary = "멤버 여행 기록 조회", description = "특정 멤버의 여행 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<MemberTravelAllResponse>> getMemberTravelRecords(
            @RequestHeader("Authorization") String accessToken) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);
        MemberTravelAllResponse rtn = memberTravelService.retrieveMemberTravel(memberId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(rtn));
    }

    @GetMapping("/listViews")
    @Operation(summary = "멤버 리스트 뷰 조회", description = "특정 멤버의 리스트 뷰를 조회합니다.")
    public ResponseEntity<ApiResponse<TravelRecordWithDiaryResponse>> getMemberListView(
            @RequestHeader("Authorization") String accessToken) {
        Long memberId = commonService.getMemberIdFromToken(accessToken);

        TravelRecordWithDiaryResponse response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
