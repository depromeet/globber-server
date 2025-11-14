package backend.globber.travelinsight.service;

import backend.globber.exception.spec.GeminiException;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.client.AiClient;
import backend.globber.travelinsight.config.TravelInsightSettings;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.domain.TravelStatistics;
import backend.globber.travelinsight.domain.TravelTitleComposer;
import backend.globber.travelinsight.domain.constant.TitleStrategy;
import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelInsightService {

    private final AiClient aiClient;
    private final MemberTravelRepository memberTravelRepository;
    private final TravelInsightRepository travelInsightRepository;
    private final TravelStatisticsService travelStatisticsService;
    private final TravelTitleComposer travelTitleComposer;
    private final TravelInsightSettings travelInsightSettings;

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

            TravelStatistics statistics = travelStatisticsService.calculate(memberId);
            if (!statistics.hasTravel()) {
                log.warn("여행 통계를 계산할 수 없어 기본 인사이트 반환 - memberId: {}", memberId);
                return TravelInsightResponse.empty();
            }

            TravelLevel level = TravelLevel.determineLevel(statistics.getCountryCount(), statistics.getCityCount());
            TravelScope scope = TravelScope.determineScope(statistics.getCountryCount(), statistics.getContinentCount());
            TravelType type = TravelType.determineType(level, statistics.getPhotoTagCounts());

            log.info("여행 통계 - memberId: {}, countryCount: {}, cityCount: {}, continentCount: {}, level: {}, scope: {}, type: {}",
                memberId, statistics.getCountryCount(), statistics.getCityCount(), statistics.getContinentCount(),
                level.name(), scope.name(), type.name());

            String resolvedTitle = resolveTitle(memberTravelAllResponse, statistics, level, scope, type);

            if (savedTravelInsight != null) {
                savedTravelInsight.updateTitle(resolvedTitle);
                log.info("기존 인사이트 업데이트 - memberId: {}, newTitle: {}", memberId, resolvedTitle);
            } else {
                TravelInsight insight = TravelInsight.builder()
                    .memberId(memberId)
                    .title(resolvedTitle)
                    .build();
                travelInsightRepository.save(insight);
                log.info("새 인사이트 생성 - memberId: {}, title: {}", memberId, resolvedTitle);
            }
            return TravelInsightResponse.of(resolvedTitle);
        } catch (Exception e) {
            log.error("예상치 못한 오류 - memberId: {}, 원인: {}", memberId, e.getMessage(), e);
            return TravelInsightResponse.empty();
        }
    }

    private String resolveTitle(
        MemberTravelAllResponse memberTravelAllResponse,
        TravelStatistics statistics,
        TravelLevel level,
        TravelScope scope,
        TravelType type
    ) {
        String serverTitle = travelTitleComposer.compose(level, scope, type);
        TitleStrategy strategy = travelInsightSettings.getTitleStrategy();

        return switch (strategy) {
            case SERVER -> serverTitle;
            case AI, HYBRID -> requestAiTitle(memberTravelAllResponse, statistics)
                .orElse(serverTitle);
        };
    }

    private java.util.Optional<String> requestAiTitle(MemberTravelAllResponse memberTravelAllResponse, TravelStatistics statistics) {
        try {
            TravelInsightResponse aiResponse = aiClient.createTitle(memberTravelAllResponse, statistics);
            if (aiResponse == null) {
                return java.util.Optional.empty();
            }
            String aiTitle = aiResponse.title();
            if (StringUtils.hasText(aiTitle) && !"자유로운 여행자".equals(aiTitle)) {
                return java.util.Optional.of(aiTitle);
            }
            log.warn("AI 인사이트가 비어있거나 기본값입니다. 서버 조합 타이틀을 사용합니다.");
        } catch (GeminiException e) {
            log.error("Gemini API 호출 실패 - 원인: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 타이틀 생성 중 예상치 못한 오류 - 원인: {}", e.getMessage(), e);
        }
        return java.util.Optional.empty();
    }
}
