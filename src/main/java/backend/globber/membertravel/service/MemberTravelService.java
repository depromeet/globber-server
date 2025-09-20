package backend.globber.membertravel.service;

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

  private final GeocodingService geocodingService;

  @Transactional
  public MemberTravelResponse saveTravelRecord(Long memberId, CreateMemberTravelRequest request) {
    String countryCode = CountryCodeConverter.convertToIso3Code(request.countryName());

    MemberTravel memberTravel = MemberTravel.builder()
        .memberId(memberId)
        .countryCode(countryCode)
        .cityName(request.cityName())
//          .lat(0.0)
//          .lng(0.0)
        .build();
    memberTravelRepository.save(memberTravel);

    log.info("여행 기록 저장 완료 - 회원ID: {}, 국가: {}, 도시: {}", memberId, request.countryName(), request.cityName());

    return MemberTravelResponse.builder()
        .countries(List.of())
        .build();
  }

  public MemberTravelResponse getMemberTravelRecords(Long memberId) {
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
  }
}
