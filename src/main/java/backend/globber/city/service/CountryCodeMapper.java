package backend.globber.city.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CountryCodeMapper {

    private static final Map<String, String> EN_TO_CODE = new HashMap<>();

    static {
        // ── 동아시아 ──────────────────────────────────────
        put("japan", "JPN");
        put("china", "CHN");
        put("taiwan", "TWN");
        put("south korea", "KOR");
        put("korea", "KOR");
        put("north korea", "PRK");
        put("mongolia", "MNG");
        put("hong kong", "HKG");
        put("macau", "MAC");
        put("macao", "MAC");

        // ── 동남아시아 ────────────────────────────────────
        put("thailand", "THA");
        put("vietnam", "VNM");
        put("viet nam", "VNM");
        put("singapore", "SGP");
        put("malaysia", "MYS");
        put("indonesia", "IDN");
        put("philippines", "PHL");
        put("cambodia", "KHM");
        put("myanmar", "MMR");
        put("burma", "MMR");
        put("laos", "LAO");
        put("lao", "LAO");
        put("brunei", "BRN");
        put("timor-leste", "TLS");
        put("east timor", "TLS");

        // ── 남아시아 ──────────────────────────────────────
        put("india", "IND");
        put("pakistan", "PAK");
        put("bangladesh", "BGD");
        put("sri lanka", "LKA");
        put("nepal", "NPL");
        put("bhutan", "BTN");
        put("maldives", "MDV");
        put("afghanistan", "AFG");

        // ── 중앙아시아 ────────────────────────────────────
        put("kazakhstan", "KAZ");
        put("uzbekistan", "UZB");
        put("kyrgyzstan", "KGZ");
        put("tajikistan", "TJK");
        put("turkmenistan", "TKM");

        // ── 서아시아 / 중동 ───────────────────────────────
        put("united arab emirates", "ARE");
        put("uae", "ARE");
        put("dubai", "ARE");
        put("saudi arabia", "SAU");
        put("ksa", "SAU");
        put("israel", "ISR");
        put("jordan", "JOR");
        put("lebanon", "LBN");
        put("syria", "SYR");
        put("iraq", "IRQ");
        put("iran", "IRN");
        put("kuwait", "KWT");
        put("bahrain", "BHR");
        put("qatar", "QAT");
        put("oman", "OMN");
        put("yemen", "YEM");
        put("turkey", "TUR");
        put("turkiye", "TUR");
        put("cyprus", "CYP");
        put("georgia", "GEO");
        put("armenia", "ARM");
        put("azerbaijan", "AZE");

        // ── 중앙유럽 ──────────────────────────────────────
        put("germany", "DEU");
        put("france", "FRA");
        put("italy", "ITA");
        put("spain", "ESP");
        put("portugal", "PRT");
        put("netherlands", "NLD");
        put("holland", "NLD");
        put("belgium", "BEL");
        put("luxembourg", "LUX");
        put("switzerland", "CHE");
        put("austria", "AUT");
        put("liechtenstein", "LIE");

        // ── 북유럽 ────────────────────────────────────────
        put("sweden", "SWE");
        put("norway", "NOR");
        put("denmark", "DNK");
        put("finland", "FIN");
        put("iceland", "ISL");
        put("estonia", "EST");
        put("latvia", "LVA");
        put("lithuania", "LTU");

        // ── 영국 / 아일랜드 ───────────────────────────────
        put("united kingdom", "GBR");
        put("uk", "GBR");
        put("great britain", "GBR");
        put("britain", "GBR");
        put("england", "GBR");
        put("scotland", "GBR");
        put("wales", "GBR");
        put("northern ireland", "GBR");
        put("ireland", "IRL");

        // ── 동유럽 ────────────────────────────────────────
        put("russia", "RUS");
        put("ukraine", "UKR");
        put("poland", "POL");
        put("czech republic", "CZE");
        put("czechia", "CZE");
        put("slovakia", "SVK");
        put("hungary", "HUN");
        put("romania", "ROU");
        put("bulgaria", "BGR");
        put("moldova", "MDA");
        put("belarus", "BLR");

        // ── 발칸 ──────────────────────────────────────────
        put("serbia", "SRB");
        put("croatia", "HRV");
        put("slovenia", "SVN");
        put("bosnia and herzegovina", "BIH");
        put("bosnia", "BIH");
        put("north macedonia", "MKD");
        put("macedonia", "MKD");
        put("albania", "ALB");
        put("montenegro", "MNE");
        put("kosovo", "XKX");
        put("greece", "GRC");

        // ── 남유럽 ────────────────────────────────────────
        put("malta", "MLT");
        put("andorra", "AND");
        put("monaco", "MCO");
        put("san marino", "SMR");
        put("vatican", "VAT");
        put("vatican city", "VAT");

        // ── 북아메리카 ────────────────────────────────────
        put("united states", "USA");
        put("united states of america", "USA");
        put("usa", "USA");
        put("us", "USA");
        put("america", "USA");
        put("canada", "CAN");
        put("mexico", "MEX");

        // ── 중앙아메리카 / 카리브해 ───────────────────────
        put("guatemala", "GTM");
        put("belize", "BLZ");
        put("honduras", "HND");
        put("el salvador", "SLV");
        put("nicaragua", "NIC");
        put("costa rica", "CRI");
        put("panama", "PAN");
        put("cuba", "CUB");
        put("jamaica", "JAM");
        put("haiti", "HTI");
        put("dominican republic", "DOM");
        put("puerto rico", "PRI");
        put("trinidad and tobago", "TTO");
        put("trinidad", "TTO");
        put("barbados", "BRB");
        put("bahamas", "BHS");

        // ── 남아메리카 ────────────────────────────────────
        put("brazil", "BRA");
        put("brasil", "BRA");
        put("argentina", "ARG");
        put("colombia", "COL");
        put("chile", "CHL");
        put("peru", "PER");
        put("venezuela", "VEN");
        put("ecuador", "ECU");
        put("bolivia", "BOL");
        put("paraguay", "PRY");
        put("uruguay", "URY");
        put("guyana", "GUY");
        put("suriname", "SUR");

        // ── 북아프리카 ────────────────────────────────────
        put("egypt", "EGY");
        put("morocco", "MAR");
        put("algeria", "DZA");
        put("tunisia", "TUN");
        put("libya", "LBY");
        put("sudan", "SDN");

        // ── 서아프리카 ────────────────────────────────────
        put("nigeria", "NGA");
        put("ghana", "GHA");
        put("senegal", "SEN");
        put("ivory coast", "CIV");
        put("cote d'ivoire", "CIV");
        put("mali", "MLI");
        put("guinea", "GIN");
        put("sierra leone", "SLE");
        put("liberia", "LBR");
        put("togo", "TGO");
        put("benin", "BEN");
        put("burkina faso", "BFA");
        put("mauritania", "MRT");
        put("niger", "NER");
        put("cape verde", "CPV");
        put("cabo verde", "CPV");
        put("gambia", "GMB");
        put("guinea-bissau", "GNB");

        // ── 중앙아프리카 ──────────────────────────────────
        put("cameroon", "CMR");
        put("chad", "TCD");
        put("central african republic", "CAF");
        put("democratic republic of the congo", "COD");
        put("drc", "COD");
        put("republic of the congo", "COG");
        put("congo", "COG");
        put("gabon", "GAB");
        put("equatorial guinea", "GNQ");
        put("sao tome and principe", "STP");

        // ── 동아프리카 ────────────────────────────────────
        put("ethiopia", "ETH");
        put("kenya", "KEN");
        put("tanzania", "TZA");
        put("uganda", "UGA");
        put("rwanda", "RWA");
        put("burundi", "BDI");
        put("somalia", "SOM");
        put("djibouti", "DJI");
        put("eritrea", "ERI");
        put("south sudan", "SSD");
        put("mozambique", "MOZ");
        put("madagascar", "MDG");
        put("mauritius", "MUS");
        put("seychelles", "SYC");
        put("comoros", "COM");
        put("malawi", "MWI");
        put("zambia", "ZMB");
        put("zimbabwe", "ZWE");

        // ── 남아프리카 ────────────────────────────────────
        put("south africa", "ZAF");
        put("namibia", "NAM");
        put("botswana", "BWA");
        put("lesotho", "LSO");
        put("eswatini", "SWZ");
        put("swaziland", "SWZ");
        put("angola", "AGO");

        // ── 오세아니아 ────────────────────────────────────
        put("australia", "AUS");
        put("new zealand", "NZL");
        put("papua new guinea", "PNG");
        put("fiji", "FJI");
        put("solomon islands", "SLB");
        put("vanuatu", "VUT");
        put("samoa", "WSM");
        put("tonga", "TON");
        put("kiribati", "KIR");
        put("palau", "PLW");
        put("micronesia", "FSM");
        put("marshall islands", "MHL");
        put("nauru", "NRU");
        put("tuvalu", "TUV");
    }

    private static void put(String name, String code) {
        EN_TO_CODE.put(name, code);
    }

    private CountryCodeMapper() {
    }

    /**
     * 영어 국가명을 ISO 3166-1 Alpha-3 코드로 변환합니다.
     * 매핑이 없으면 Optional.empty()를 반환합니다.
     */
    public static Optional<String> toCountryCode(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(EN_TO_CODE.get(keyword.toLowerCase().trim()));
    }
}
