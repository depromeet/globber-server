package backend.globber.travelinsight.domain.constant;

import lombok.Getter;

@Getter
public enum TravelLevel {
    LEVEL_0("섬세한", 0),      // 깊이형
    LEVEL_1("열정적인", 1),    // 탐구형
    LEVEL_2("호기심 많은", 2), // 활동형
    LEVEL_3("대담한", 3);      // 탐험형

    private final String adjective;
    private final int level;

    TravelLevel(String adjective, int level) {
        this.adjective = adjective;
        this.level = level;
    }

    /**
     * 여행 규모에 따라 레벨 결정
     *
     * @param countryCount 국가 수
     * @param cityCount    도시 수
     * @return TravelLevel
     */
    public static TravelLevel determineLevel(int countryCount, int cityCount) {
        // Lv.3 (탐험형): 국가 15개 이상 또는 도시 30개 이상
        if (countryCount >= 15 || cityCount >= 30) {
            return LEVEL_3;
        }

        // Lv.2 (활동형): 국가 5~14개 또는 도시 15~29개
        if ((countryCount >= 5 && countryCount <= 14) || (cityCount >= 15 && cityCount <= 29)) {
            return LEVEL_2;
        }

        // Lv.1 (탐구형): 국가 2~4개 또는 도시 8~14개
        if ((countryCount >= 2 && countryCount <= 4) || (cityCount >= 8 && cityCount <= 14)) {
            return LEVEL_1;
        }

        // Lv.0 (깊이형): 국가 1개 또는 도시 3~7개
        return LEVEL_0;
    }
}
