package backend.globber.membertravel.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CountryInfo {

  private String code;      // ISO 3166-1 Alpha-3 코드
  private String cityName;    // 도시명
  private Double lat;     // 위도
  private Double lng;     // 경도
}
