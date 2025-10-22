package backend.globber.membertravel.service;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Continent {
    ASIA("아시아", List.of("KOR", "JPN", "CHN", "THA", "VNM")),
    EUROPE("유럽", List.of("GBR", "FRA", "DEU", "ITA", "ESP")),
    NORTH_AMERICA("북미", List.of("USA", "CAN", "MEX")),
    SOUTH_AMERICA("남미", List.of("BRA", "ARG", "CHL")),
    OCEANIA("오세아니아", List.of("AUS", "NZL")),
    AFRICA("아프리카", List.of("ZAF", "EGY", "KEN")),
    ANTARCTICA("남극", List.of()),
    OTHER("기타", List.of());

    private final String displayName;
    private final List<String> countryCodes;

    Continent(String displayName, List<String> countryCodes) {
        this.displayName = displayName;
        this.countryCodes = List.copyOf(countryCodes);
    }

    public static Continent fromCountryCode(String code) {
        return Arrays.stream(values())
                .filter(continent -> continent.countryCodes.contains(code))
                .findFirst()
                .orElse(OTHER);
    }
}
