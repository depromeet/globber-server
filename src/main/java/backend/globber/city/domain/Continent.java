package backend.globber.city.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 대륙 분류 Enum
 */
public enum Continent {
    ASIA(Arrays.asList(
            "KOR", "JPN", "CHN", "TWN", "HKG", "MAC", "MNG", "PRK",  // 동아시아
            "THA", "VNM", "SGP", "MYS", "IDN", "PHL", "BRN", "MMR", "KHM", "LAO", "TLS",  // 동남아시아
            "IND", "PAK", "BGD", "LKA", "NPL", "BTN", "MDV", "AFG",  // 남아시아
            "SAU", "ARE", "QAT", "KWT", "BHR", "OMN", "YEM", "JOR", "LBN", "SYR", "IRQ", "IRN", "ISR", "PSE", "TUR", "CYP", "ARM", "AZE", "GEO"  // 서아시아/중동
    )),
    EUROPE(Arrays.asList(
            "GBR", "FRA", "DEU", "ITA", "ESP", "PRT", "NLD", "BEL", "LUX", "CHE", "AUT",  // 서유럽
            "POL", "CZE", "SVK", "HUN", "ROU", "BGR", "SVN", "HRV", "BIH", "SRB", "MNE", "MKD", "ALB", "GRC",  // 중동부 유럽
            "DNK", "SWE", "NOR", "FIN", "ISL", "EST", "LVA", "LTU",  // 북유럽, 발트
            "UKR", "BLR", "RUS", "MDA"  // 동유럽
    )),
    NORTH_AMERICA(Arrays.asList(
            "USA", "CAN", "MEX", "CUB", "JAM", "HTI", "DOM", "PRI", "TTO", "BHS", "BRB", "ATG", "DMA", "GRD", "KNA", "LCA", "VCT",  // 북미/카리브
            "GTM", "BLZ", "SLV", "HND", "NIC", "CRI", "PAN"  // 중앙아메리카
    )),
    SOUTH_AMERICA(Arrays.asList(
            "BRA", "ARG", "CHL", "COL", "PER", "VEN", "ECU", "BOL", "PRY", "URY", "GUY", "SUR", "GUF"
    )),
    AFRICA(Arrays.asList(
            "EGY", "MAR", "ZAF", "NGA", "KEN", "ETH", "GHA", "TZA", "UGA", "DZA", "TUN", "LBY", "SEN", "CIV", "CMR", "BFA", "MLI", "NER", "TCD", "SDN", "SSD", "SOM", "MDG", "MOZ", "ZWE", "ZMB", "MWI", "RWA", "BDI", "AGO", "NAM", "BWA", "LSO", "SWZ", "GAB", "GNQ", "COG", "COD", "CAF", "TGO", "BEN", "GNB", "GMB", "SLE", "LBR"
    )),
    OCEANIA(Arrays.asList(
            "AUS", "NZL", "FJI", "PNG", "NCL", "SLB", "VUT", "WSM", "TON", "KIR", "FSM", "MHL", "PLW", "NRU", "TUV"
    )),
    ANTARCTICA(Arrays.asList("ATA"));

    private final List<String> countryCodes;

    Continent(List<String> countryCodes) {
        this.countryCodes = countryCodes;
    }

    /**
     * 국가 코드로 대륙 찾기
     * @param countryCode ISO 3166-1 Alpha-3 국가 코드
     * @return Continent (찾지 못하면 null)
     */
    public static Continent fromCountryCode(String countryCode) {
        for (Continent continent : values()) {
            if (continent.countryCodes.contains(countryCode)) {
                return continent;
            }
        }
        // 매핑되지 않은 국가의 경우 null 반환 (또는 기본값 설정 가능)
        return null;
    }
}
