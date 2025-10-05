package backend.globber.travelinsight.client;

import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;

public interface AiClient {

    TravelInsightResponse createTitle(MemberTravelAllResponse travels);
}
