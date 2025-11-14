package backend.globber.travelinsight.client;

import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelStatistics;

public interface AiClient {

    TravelInsightResponse createTitle(MemberTravelAllResponse travels, TravelStatistics statistics);
}
