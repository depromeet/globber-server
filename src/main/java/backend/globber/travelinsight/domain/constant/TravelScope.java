package backend.globber.travelinsight.domain.constant;

import lombok.Getter;

@Getter
public enum TravelScope {
    CULTURE("문화"),   // 1개 국가 집중
    CONTINENT("대륙"), // 1개 대륙 내 4~9개 국가
    WORLD("세계");     // 국가 10개 이상 또는 3개 대륙 이상

    private final String scopeName;

    TravelScope(String scopeName) {
        this.scopeName = scopeName;
    }

    /**
     * 여행 범위에 따라 스코프 결정
     * @param countryCount 국가 수
     * @param continentCount 대륙 수
     * @return TravelScope
     */
    public static TravelScope determineScope(int countryCount, int continentCount) {
        // 세계: 국가 10개 이상 또는 3개 대륙 이상
        if (countryCount >= 10 || continentCount >= 3) {
            return WORLD;
        }
        // 대륙: 1개 대륙 내 4~9개 국가
        if (continentCount == 1 && countryCount >= 4 && countryCount <= 9) {
            return CONTINENT;
        }
        // 문화: 1개 국가 집중
        return CULTURE;
    }
}
