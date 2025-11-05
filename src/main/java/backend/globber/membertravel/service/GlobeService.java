package backend.globber.membertravel.service;

import backend.globber.exception.spec.UUIDNotFoundException;
import backend.globber.membertravel.controller.dto.CityDto;
import backend.globber.membertravel.controller.dto.GlobeSummaryDto;
import backend.globber.membertravel.controller.dto.RegionDto;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobeService {

    private final MemberTravelRepository memberTravelRepository;

    @Transactional(readOnly = true)
    public GlobeSummaryDto getGlobe(final String uuid) {
        MemberTravel memberTravel = memberTravelRepository.findByMember_Uuid(uuid)
                .orElseThrow(() -> new UUIDNotFoundException("해당 uuid의 지구본이 존재하지 않습니다."));

        List<MemberTravelCity> travelCities = memberTravel.getMemberTravelCities();

        int cityCount = travelCities.size();
        long countryCount = getCount(travelCities);

        Map<String, List<MemberTravelCity>> groupedByCountry = getStringListMap(travelCities);
        List<RegionDto> regions = getRegionDtos(groupedByCountry);

        return new GlobeSummaryDto(memberTravel.getMember().getName(), cityCount, (int) countryCount, regions);
    }

    private static long getCount(List<MemberTravelCity> travelCities) {
        return travelCities.stream()
                .map(tc -> tc.getCity().getCountryName())
                .distinct()
                .count();
    }

    private static Map<String, List<MemberTravelCity>> getStringListMap(List<MemberTravelCity> travelCities) {
        Map<String, List<MemberTravelCity>> groupedByCountry = travelCities.stream()
                .collect(Collectors.groupingBy(tc -> tc.getCity().getCountryName()));
        return groupedByCountry;
    }

    private static List<RegionDto> getRegionDtos(Map<String, List<MemberTravelCity>> groupedByCountry) {
        List<RegionDto> regions = groupedByCountry.entrySet().stream()
                .map(entry -> {
                    String countryName = entry.getKey();
                    List<CityDto> cities = entry.getValue().stream()
                            .map(tc -> new CityDto(
                                    tc.getCity().getCityName(),
                                    tc.getCity().getLat(),
                                    tc.getCity().getLng(),
                                    tc.getCity().getCountryCode()
                            ))
                            .toList();
                    return new RegionDto(countryName, cities.size(), cities);
                })
                .toList();
        return regions;
    }
}
