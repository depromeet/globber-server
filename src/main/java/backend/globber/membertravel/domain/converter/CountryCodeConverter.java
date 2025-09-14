package backend.globber.membertravel.domain.converter;

import com.ibm.icu.util.ULocale;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class CountryCodeConverter {

  private static final String[] ISO_COUNTRIES_ALL = ULocale.getISOCountries();
  private static final ULocale KOREAN_LOCALE = new ULocale("ko_KR");

  /**
   * 한국어 국가명을 ISO Alpha-3 코드로 변환
   *
   * @param countryName 한국어 국가명 (예: "이탈리아", "대한민국")
   * @return ISO Alpha-3 코드 (예: "ITA", "KOR")
   * @throws IllegalArgumentException 매칭되는 국가를 찾을 수 없는 경우
   */
  public static String convertToIso3Code(String countryName) {
    if (countryName == null || countryName.trim().isEmpty()) {
      throw new IllegalArgumentException("국가명은 null이거나 빈 문자열일 수 없습니다.");
    }

    String trimmedName = countryName.trim();

    for (String iso2 : ISO_COUNTRIES_ALL) {
      try {
        ULocale countryLocale = new ULocale("", iso2);
        String koreanName = countryLocale.getDisplayCountry(KOREAN_LOCALE);

        if (koreanName != null && koreanName.equals(trimmedName)) {
          return countryLocale.getISO3Country();
        }
      } catch (Exception e) {
        log.error("국가 코드 변환 중 오류 발생 - 국가명: {}, ISO2 코드: {}", countryName, iso2, e);
      }
    }

    throw new IllegalArgumentException("해당 국가를 찾을 수 없습니다: " + countryName);
  }
}