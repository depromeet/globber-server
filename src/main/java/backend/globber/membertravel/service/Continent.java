package backend.globber.membertravel.service;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Continent {
    ASIA("아시아", List.of(
            "AFG", "ARM", "AZE", "BHR", "BGD", "BTN", "BRN", "KHM", "CHN", "CYP", "GEO", "HKG", "IND", "IDN",
            "IRN", "IRQ", "ISR", "JPN", "JOR", "KAZ", "KWT", "KGZ", "LAO", "LBN", "MAC", "MYS", "MDV", "MNG",
            "MMR", "NPL", "PRK", "OMN", "PAK", "PSE", "PHL", "QAT", "SAU", "SGP", "KOR", "LKA", "SYR", "TWN",
            "TJK", "THA", "TLS", "TUR", "TKM", "ARE", "UZB", "VNM", "YEM"
    )),
    EUROPE("유럽", List.of(
            "ALB", "AND", "AUT", "BLR", "BEL", "BIH", "BGR", "HRV", "CZE", "DNK", "EST", "FRO", "FIN", "FRA",
            "DEU", "GIB", "GRC", "GGY", "HUN", "ISL", "IRL", "IMN", "ITA", "JEY", "LVA", "LIE", "LTU", "LUX",
            "MLT", "MDA", "MCO", "MNE", "NLD", "MKD", "NOR", "POL", "PRT", "ROU", "SMR", "SRB", "SVK", "SVN",
            "ESP", "SWE", "CHE", "UKR", "GBR", "VAT"
    )),
    NORTH_AMERICA("북미", List.of(
            "AIA", "ATG", "BHS", "BRB", "BLZ", "BMU", "CAN", "CYM", "CRI", "CUB", "DMA", "DOM", "SLV", "GRD",
            "GLP", "GTM", "HTI", "HND", "JAM", "MTQ", "MEX", "MSR", "ANT", "CUW", "KNA", "LCA", "VCT", "SPM",
            "TTO", "TCA", "USA", "VIR", "PRI", "GUF", "GRL", "BLM", "MAF"
    )),
    SOUTH_AMERICA("남미", List.of(
            "ARG", "BOL", "BRA", "CHL", "COL", "ECU", "GUY", "PRY", "PER", "SUR", "URY", "VEN", "FLK"
    )),
    OCEANIA("오세아니아", List.of(
            "ASM", "AUS", "COK", "FJI", "PYF", "GUM", "KIR", "MHL", "FSM", "NRU", "NCL", "NZL", "NIU", "NFK",
            "MNP", "PLW", "PNG", "WSM", "SLB", "TKL", "TON", "TUV", "VUT", "WLF"
    )),
    AFRICA("아프리카", List.of(
            "DZA", "AGO", "BEN", "BWA", "BFA", "BDI", "CMR", "CPV", "CAF", "TCD", "COM", "COG", "COD", "DJI",
            "EGY", "GNQ", "ERI", "SWZ", "ETH", "GAB", "GMB", "GHA", "GIN", "GNB", "CIV", "KEN", "LSO", "LBR",
            "LBY", "MDG", "MWI", "MLI", "MRT", "MUS", "MYT", "MAR", "MOZ", "NAM", "NER", "NGA", "REU", "RWA",
            "STP", "SEN", "SYC", "SLE", "SOM", "ZAF", "SSD", "SDN", "TZA", "TGO", "TUN", "UGA", "ZMB", "ZWE"
    )),
    ANTARCTICA("남극", List.of(
            "ATA", "BVT", "HMD", "SGS", "ATF"
    )),
    OTHER("기타", List.of(
            "ESH"
    ));

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