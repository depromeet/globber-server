package backend.globber.travelinsight.service;

import backend.globber.city.domain.Continent;
import backend.globber.travelinsight.domain.TravelStatistics;
import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 여행 타이틀 생성기
 * 사용자의 여행 통계를 기반으로 타이틀을 생성합니다.
 */
@Slf4j
@Component
public class TravelTitleGenerator {

    private static final String DEFAULT_TITLE = "자유로운 여행자";

    /**
     * 여행 통계를 기반으로 타이틀 생성
     * @param statistics 여행 통계
     * @param visitedCountryCodes 방문한 국가 코드 리스트
     * @return 생성된 타이틀 (예: "대담한 세계 탐험가")
     */
    public String generateTitle(TravelStatistics statistics, List<String> visitedCountryCodes) {
        // 여행 기록이 없는 경우 기본 타이틀 반환
        if (!statistics.hasTravel()) {
            log.debug("여행 기록이 없어 기본 타이틀 반환: {}", DEFAULT_TITLE);
            return DEFAULT_TITLE;
        }

        // 1. 여행 레벨 결정 (형용사)
        TravelLevel level = TravelLevel.determineLevel(
                statistics.getCountryCount(),
                statistics.getCityCount()
        );
        String adjective = level.getAdjective();
        log.debug("여행 레벨: {} (국가: {}, 도시: {})", level, statistics.getCountryCount(), statistics.getCityCount());

        // 2. 대륙 수 계산
        int continentCount = calculateContinentCount(visitedCountryCodes);
        log.debug("대륙 수: {}", continentCount);

        // 3. 여행 범위 결정 (두번째 단어)
        TravelScope scope = TravelScope.determineScope(
                statistics.getCountryCount(),
                continentCount
        );
        String scopeName = scope.getScopeName();
        log.debug("여행 범위: {}", scope);

        // 4. 여행 타입 결정 (세번째 단어)
        TravelType type = TravelType.determineType(level, statistics.getPhotoTagCounts());
        String typeName = type.getTypeName();
        log.debug("여행 타입: {} (사진 태그: {})", type, statistics.getPhotoTagCounts());

        // 5. 타이틀 조합
        String title = adjective + " " + scopeName + " " + typeName;
        log.info("생성된 타이틀: '{}'", title);

        return title;
    }

    /**
     * 방문한 국가 코드들로부터 대륙 수 계산
     * @param countryCodes 국가 코드 리스트
     * @return 고유 대륙 수
     */
    private int calculateContinentCount(List<String> countryCodes) {
        if (countryCodes == null || countryCodes.isEmpty()) {
            return 0;
        }

        Set<Continent> continents = countryCodes.stream()
                .map(Continent::fromCountryCode)
                .filter(continent -> continent != null) // 매핑되지 않은 국가 제외
                .collect(Collectors.toSet());

        return continents.size();
    }
}
