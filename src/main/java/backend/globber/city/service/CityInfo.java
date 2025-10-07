package backend.globber.city.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CityInfo {

    TOKYO("도쿄", "일본", 35.682839, 139.759455, "JPN"),
    OSAKA("오사카", "일본", 34.693725, 135.502254, "JPN"),
    FUKUOKA("후쿠오카", "일본", 33.590355, 130.401716, "JPN"),
    SAPPORO("삿포로", "일본", 43.061936, 141.354292, "JPN"),

    BANGKOK("방콕", "태국", 13.756331, 100.501765, "THA"),
    PATTAYA("파타야", "태국", 12.923556, 100.882455, "THA"),
    PHUKET("푸껫", "태국", 7.880448, 98.392293, "THA"),

    HANOI("하노이", "베트남", 21.027763, 105.834160, "VNM"),
    HOCHIMINH("호치민시", "베트남", 10.776530, 106.700981, "VNM"),
    DANANG("다낭", "베트남", 16.054407, 108.202167, "VNM"),

    GUAM("괌", "괌", 13.444304, 144.793731, "GUM"),
    SAIPAN("사이판", "북마리아나 제도", 15.177801, 145.750967, "MNP"),

    LOS_ANGELES("로스앤젤레스", "미국", 34.052235, -118.243683, "USA"),
    NEW_YORK("뉴욕", "미국", 40.712776, -74.005974, "USA"),
    LAS_VEGAS("라스베이거스", "미국", 36.169941, -115.139832, "USA"),

    PARIS("파리", "프랑스", 48.856613, 2.352222, "FRA"),
    LONDON("런던", "영국", 51.507351, -0.127758, "GBR"),
    ROME("로마", "이탈리아", 41.902782, 12.496366, "ITA"),
    BARCELONA("바르셀로나", "스페인", 41.385064, 2.173404, "ESP"),

    SINGAPORE("싱가포르", "싱가포르", 1.352083, 103.819839, "SGP");

    private final String cityName;
    private final String countryName;
    private final double lat;
    private final double lng;
    private final String countryCode;
}
