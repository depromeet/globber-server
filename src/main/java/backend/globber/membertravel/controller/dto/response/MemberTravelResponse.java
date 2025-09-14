package backend.globber.membertravel.controller.dto.response;

import backend.globber.membertravel.controller.dto.CountryInfo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberTravelResponse {

  private List<CountryInfo> countries;
}
