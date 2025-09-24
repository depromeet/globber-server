package backend.globber.travelinsight.service;

import backend.globber.exception.spec.GeminiException;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.client.GeminiApiClient;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        TravelInsight savedTravelInsight = findTravelInsight(memberId);

        List<MemberTravel> travels = memberTravelRepository.findAllByMember_Id(memberId);
        if (travels.isEmpty()) {
            return TravelInsightResponse.empty();
        }

        if (savedTravelInsight != null && !isTravelDataChanged(savedTravelInsight, travels)) {
            log.info("기존 캐시된 인사이트 반환 - memberId: {}", memberId);
            return TravelInsightResponse.of(savedTravelInsight.getTitle());
        }

        return createOrUpdateTravelInsight(memberId, travels, savedTravelInsight);
    }

    private TravelInsight findTravelInsight(Long memberId) {
        return travelInsightRepository.findByMemberId(memberId)
            .orElse(null);
    }

    private boolean isTravelDataChanged(TravelInsight savedInsight, List<MemberTravel> travels) {
        LocalDateTime insightUpdatedAt = savedInsight.getUpdatedAt();

        LocalDateTime latestTravelUpdate = travels.stream()
            .map(MemberTravel::getUpdatedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return latestTravelUpdate == null || latestTravelUpdate.isAfter(insightUpdatedAt);
    }

    private TravelInsightResponse createOrUpdateTravelInsight(Long memberId, List<MemberTravel> travels, TravelInsight savedTravelInsight) {
        try {
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
        } catch (GeminiException e) {
            log.error("Gemini API 호출 실패 - memberId: {}, 원인: {}", memberId, e.getMessage(), e);
            return TravelInsightResponse.empty(); // 일단 빈 응답 반환
        }
    }
}
