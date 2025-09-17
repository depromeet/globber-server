package backend.globber.membertravel.service;

import backend.globber.membertravel.controller.dto.CityCoordinates;
import backend.globber.membertravel.controller.dto.CountryInfo;
import backend.globber.membertravel.controller.dto.request.CreateMemberTravelRequest;
import backend.globber.membertravel.controller.dto.response.MemberTravelResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.converter.CountryCodeConverter;
import backend.globber.membertravel.repository.MemberTravelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTravelService {

  private final MemberTravelRepository memberTravelRepository;

  private final CityCoordinatesRedisService redisService;
  private final GeocodingService geocodingService;

  public MemberTravelResponse saveTravelRecord(Long memberId, CreateMemberTravelRequest request) {
    try {
      String countryCode = CountryCodeConverter.convertToIso3Code(request.countryName());

      CityCoordinates coordinates = redisService.getCityCoordinates(request.cityName(), request.countryName());

      // Redis에 없으면 외부 API 호출?? 일단 mock
      if (coordinates == null) {
        coordinates = geocodingService.getCoordinates(request.cityName(), request.countryName());

        // Redis에 캐싱
        if (coordinates != null) {
          redisService.saveCityCoordinates(request.cityName(), request.countryName(), coordinates);
        }
      }

      if (coordinates == null) {
        throw new IllegalArgumentException(
            "해당 도시의 좌표를 찾을 수 없습니다: " + request.cityName() + ", " + request.countryName());
      }

      MemberTravel memberTravel = MemberTravel.builder()
          .memberId(memberId)
          .countryCode(countryCode)
          .cityName(request.cityName())
          .lat(coordinates.lat())
          .lng(coordinates.lng())
          .build();

      memberTravelRepository.save(memberTravel);

      log.info("여행 기록 저장 완료 - 회원ID: {}, 국가: {}, 도시: {}",
          memberId, request.countryName(), request.cityName());

    } catch (Exception e) {
      log.error("여행 기록 저장 실패 - 회원ID: {}, 요청: {}", memberId, request, e);
      throw new RuntimeException("여행 기록 저장에 실패했습니다.", e);
    }

    return null;
  }

  public MemberTravelResponse getMemberTravelRecords(Long memberId) {
    try {
      List<MemberTravel> memberTravels = memberTravelRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

      List<CountryInfo> countries = memberTravels.stream()
          .map(memberTravel -> CountryInfo.builder()
              .code(memberTravel.getCountryCode())
              .cityName(memberTravel.getCityName())
              .lat(memberTravel.getLat())
              .lng(memberTravel.getLng())
              .build())
          .toList();

      return MemberTravelResponse.builder()
          .countries(countries)
          .build();

    } catch (Exception e) {
      log.error("지구본 데이터 조회 실패 - 회원ID: {}", memberId, e);
      throw new RuntimeException("여행 기록 조회에 실패했습니다.", e);
    }
  }
}
