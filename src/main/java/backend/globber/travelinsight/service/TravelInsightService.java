package backend.globber.travelinsight.service;

import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.client.GeminiApiClient;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelInsightService {

    private final GeminiApiClient aiClient;
    private final MemberTravelRepository memberTravelRepository;
    private final TravelInsightRepository travelInsightRepository;

    @Transactional
    public TravelInsightResponse getOrCreateInsight(Long memberId) {
        try {
            TravelInsight savedTravelInsight = travelInsightRepository.findByMemberId(memberId).orElse(null);

            List<MemberTravel> travels = memberTravelRepository.findAllByMember_Id(memberId);
            if (travels.isEmpty()) {
                return TravelInsightResponse.empty();
            }

            if (savedTravelInsight != null && !isTravelDataChanged(savedTravelInsight, travels)) {
                log.info("기존 캐시된 인사이트 반환 - memberId: {}", memberId);
                return TravelInsightResponse.builder()
                    .title(savedTravelInsight.getTitle())
                    .build();
            }

            MemberTravelAllResponse memberTravelAllResponse = MemberTravelAllResponse.from(memberId, travels);

            log.info("새로운 인사이트 생성 중 - memberId: {}, 여행 기록 수: {}", memberId, travels.size());
            TravelInsightResponse newInsight = aiClient.createTitle(memberTravelAllResponse);

            if (savedTravelInsight != null) {
                savedTravelInsight.updateTitle(newInsight.title());
                log.info("기존 인사이트 업데이트 - memberId: {}, newTitle: {}", memberId, newInsight.title());
            } else {
                TravelInsight insight = TravelInsight.builder()
                    .memberId(memberId)
                    .title(newInsight.title())
                    .build();
                travelInsightRepository.save(insight);
                log.info("새 인사이트 생성 - memberId: {}, title: {}", memberId, newInsight.title());
            }

            return newInsight;

        } catch (Exception e) {
            log.error("AI 연동 실패: {}", e.getMessage(), e);
            return TravelInsightResponse.builder()
                .title("여행 초보자")
                .build();
        }
    }

    private boolean isTravelDataChanged(TravelInsight savedInsight, List<MemberTravel> travels) {
        LocalDateTime insightUpdatedAt = savedInsight.getUpdatedAt();

        LocalDateTime latestTravelUpdate = travels.stream()
            .map(MemberTravel::getUpdatedAt)
            .filter(updatedAt -> updatedAt != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return latestTravelUpdate == null || latestTravelUpdate.isAfter(insightUpdatedAt);
    }
}
