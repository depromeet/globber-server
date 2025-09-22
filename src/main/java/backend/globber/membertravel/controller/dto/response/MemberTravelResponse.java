package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.TravelCityDto;
import backend.globber.membertravel.domain.MemberTravel;
import java.util.List;

public record MemberTravelResponse(
    Long memberTravelId,
    List<TravelCityDto> cities
) {
    public static MemberTravelResponse from(MemberTravel memberTravel) {
        List<TravelCityDto> cities = memberTravel.getMemberTravelCities().stream()
            .map(TravelCityDto::from)
            .toList();

        return new MemberTravelResponse(
            memberTravel.getId(),
            cities
        );
    }
}
