package backend.globber.membertravel.service;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Continent {

    아시아(List.of("KOR", "JPN", "CHN", "THA", "VNM")),
    유럽(List.of("GBR", "FRA", "DEU", "ITA", "ESP")),
    북미(List.of("USA", "CAN", "MEX")),
    남미(List.of("BRA", "ARG", "CHL")),
    오세아니아(List.of("AUS", "NZL")),
    아프리카(List.of("ZAF", "EGY", "KEN")),
    남극(List.of()),
    기타(List.of());

    private final List<String> countryCodes;

    Continent(List<String> countryCodes) {
        this.countryCodes = List.copyOf(countryCodes);
    }

    public static Continent fromCountryCode(String code) {
        return Arrays.stream(values())
                .filter(continent -> continent.getCountryCodes().contains(code))
                .findFirst()
                .orElse(기타);
    }
}
