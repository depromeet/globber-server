package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryInfo;
import java.util.List;
import lombok.Builder;

@Builder
public record MemberTravelResponse(
    List<CountryInfo> countries
) {

}
