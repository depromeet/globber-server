package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.domain.MemberTravel;
import java.util.List;

public record MemberTravelAllResponse(
    Long memberId,
    List<MemberTravelResponse> travels
) {
    public static MemberTravelAllResponse from(Long memberId, List<MemberTravel> memberTravels) {
        List<MemberTravelResponse> travels = memberTravels.stream()
            .map(MemberTravelResponse::from)
            .toList();

        return new MemberTravelAllResponse(
            memberId,
            travels
        );
    }
}
