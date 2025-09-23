package backend.globber.membertravel.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.exception.spec.CityNotFoundException;
import backend.globber.exception.spec.TravelNotFoundException;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.membertravel.controller.dto.request.CreateMemberTravelRequest;
import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberTravelService {

    private final MemberRepository memberRepository;
    private final CityRepository cityRepository;
    private final MemberTravelRepository memberTravelRepository;
    private final MemberTravelCityRepository memberTravelCityRepository;

    @CacheEvict(value = "memberTravels", key = "#memberId", cacheManager = "memberTravelCacheManager")
    @Transactional
    public MemberTravelAllResponse createMemberTravel(Long memberId, List<CreateMemberTravelRequest> requests) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        // MemberTravel이 이미 있으면 재사용, 없으면 생성
        MemberTravel memberTravel = memberTravelRepository.findByMember_Id(memberId)
            .orElseGet(() -> memberTravelRepository.save(
                MemberTravel.builder().member(member).build()
            ));

        for (CreateMemberTravelRequest req : requests) {
            CityUniqueDto dto = CityUniqueDto.builder()
                .cityName(req.cityName())
                .countryCode(req.countryCode())
                .lat(req.lat())
                .lng(req.lng())
                .build();

            City city = cityRepository.findByCityUniqueDto(dto)
                .orElseGet(() -> {
                    City newCity = City.builder()
                        .cityName(req.cityName())
                        .countryName(req.countryName())
                        .countryCode(req.countryCode())
                        .lat(req.lat())
                        .lng(req.lng())
                        .build();
                    return cityRepository.save(newCity);
                });

            boolean exists = memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(
                memberTravel.getId(), city.getCityId()
            );

            if (!exists) {
                MemberTravelCity mtc = MemberTravelCity.builder()
                    .memberTravel(memberTravel)
                    .city(city)
                    .build();

                memberTravelCityRepository.save(mtc);
                memberTravel.getMemberTravelCities().add(mtc);
            }
        }

        return MemberTravelAllResponse.from(memberId,
            memberTravelRepository.findAllByMember_Id(memberId));
    }


    @Cacheable(value = "memberTravels", key = "#memberId", cacheManager = "memberTravelCacheManager")
    @Transactional(readOnly = true)
    public MemberTravelAllResponse retrieveMemberTravel(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        List<MemberTravel> travels = memberTravelRepository.findAllByMember_Id(memberId);
        return MemberTravelAllResponse.from(memberId, travels);
    }

    @CacheEvict(value = "memberTravels", key = "#memberId", cacheManager = "memberTravelCacheManager")
    @Transactional
    public Boolean deleteTravelRecord(Long memberId, CityUniqueDto cityDto) {
        MemberTravel memberTravel = memberTravelRepository.findByMember_Id(memberId)
            .orElseThrow(TravelNotFoundException::new);

        City city = cityRepository.findByCityUniqueDto(cityDto)
            .orElseThrow(CityNotFoundException::new);

        memberTravelCityRepository.deleteByMemberTravel_IdAndCity_CityId(memberTravel.getId(), city.getCityId());
        return true;
    }
}
