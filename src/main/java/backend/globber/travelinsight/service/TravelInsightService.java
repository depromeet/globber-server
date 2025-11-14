package backend.globber.travelinsight.service;

import backend.globber.city.domain.City;
import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.domain.TravelStatistics;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelInsightService {

    private final MemberTravelRepository memberTravelRepository;
    private final MemberTravelCityRepository memberTravelCityRepository;
    private final PhotoRepository photoRepository;
    private final TravelInsightRepository travelInsightRepository;
    private final TravelTitleGenerator travelTitleGenerator;

    @Transactional
    public TravelInsightResponse getOrCreateInsight(Long memberId) {
        TravelInsight savedTravelInsight = findTravelInsight(memberId);

        List<MemberTravel> travels = memberTravelRepository.findAllByMember_Id(memberId);
        if (travels.isEmpty()) {
            log.info("여행 기록 없음 - memberId: {}, 기본 타이틀 반환", memberId);
            return TravelInsightResponse.empty();
        }

        if (savedTravelInsight != null && !isTravelDataChanged(savedTravelInsight, travels)) {
            log.info("기존 캐시된 인사이트 반환 - memberId: {}", memberId);
            return TravelInsightResponse.of(savedTravelInsight.getTitle());
        }

        return createOrUpdateTravelInsight(memberId, savedTravelInsight);
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

    private TravelInsightResponse createOrUpdateTravelInsight(Long memberId, TravelInsight savedTravelInsight) {
        try {
            // 1. 통계 데이터 및 방문 도시 수집 (한 번의 쿼리로 처리)
            TravelStatisticsWithCities statsWithCities = collectTravelStatistics(memberId);
            TravelStatistics statistics = statsWithCities.statistics();
            List<City> visitedCities = statsWithCities.visitedCities();

            log.info("여행 통계 수집 완료 - memberId: {}, 국가: {}, 도시: {}",
                    memberId, statistics.getCountryCount(), statistics.getCityCount());

            // 2. 방문 국가 코드 리스트 추출
            List<String> visitedCountryCodes = getVisitedCountryCodes(visitedCities);

            // 3. 타이틀 생성
            String title = travelTitleGenerator.generateTitle(statistics, visitedCountryCodes);
            log.info("타이틀 생성 완료 - memberId: {}, title: '{}'", memberId, title);

            // 4. DB 저장 또는 업데이트
            if (savedTravelInsight != null) {
                savedTravelInsight.updateTitle(title);
                log.info("기존 인사이트 업데이트 - memberId: {}, newTitle: '{}'", memberId, title);
            } else {
                TravelInsight insight = TravelInsight.builder()
                        .memberId(memberId)
                        .title(title)
                        .build();
                travelInsightRepository.save(insight);
                log.info("새 인사이트 생성 - memberId: {}, title: '{}'", memberId, title);
            }

            return TravelInsightResponse.of(title);
        } catch (Exception e) {
            log.error("인사이트 생성 중 오류 발생 - memberId: {}, 원인: {}", memberId, e.getMessage(), e);
            return TravelInsightResponse.empty();
        }
    }

    /**
     * 사용자의 여행 통계 데이터 수집
     * 방문한 도시 리스트를 반환하여 재사용 가능하도록 함
     */
    private TravelStatisticsWithCities collectTravelStatistics(Long memberId) {
        // 방문한 도시 리스트 조회 (한 번의 쿼리로 모든 정보 획득)
        List<City> visitedCities = memberTravelCityRepository.findVisitedCities(memberId);

        // 메모리에서 국가 수, 도시 수 계산
        int countryCount = (int) visitedCities.stream()
                .map(City::getCountryCode)
                .distinct()
                .count();

        int cityCount = visitedCities.size(); // 이미 distinct한 city 리스트

        // 사진 태그별 개수 조회
        Map<PhotoTag, Long> photoTagCounts = getPhotoTagCounts(memberId);

        TravelStatistics statistics = TravelStatistics.builder()
                .countryCount(countryCount)
                .cityCount(cityCount)
                .continentCount(0) // 대륙 수는 TravelTitleGenerator에서 계산
                .photoTagCounts(photoTagCounts)
                .build();

        return new TravelStatisticsWithCities(statistics, visitedCities);
    }

    /**
     * 방문한 도시 리스트에서 국가 코드만 추출
     */
    private List<String> getVisitedCountryCodes(List<City> visitedCities) {
        return visitedCities.stream()
                .map(City::getCountryCode)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 통계와 도시 리스트를 함께 담는 내부 DTO
     */
    private record TravelStatisticsWithCities(TravelStatistics statistics, List<City> visitedCities) {
    }

    /**
     * 사진 태그별 개수 조회
     */
    private Map<PhotoTag, Long> getPhotoTagCounts(Long memberId) {
        List<Object[]> results = photoRepository.countPhotosByTagForMember(memberId);
        Map<PhotoTag, Long> tagCounts = new HashMap<>();

        for (Object[] result : results) {
            PhotoTag tag = (PhotoTag) result[0];
            Long count = (Long) result[1];
            tagCounts.put(tag, count);
        }

        log.debug("사진 태그 통계 - memberId: {}, tagCounts: {}", memberId, tagCounts);
        return tagCounts;
    }
}
