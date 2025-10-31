package backend.globber.city.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.city.controller.dto.CityRequest;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.exception.spec.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final RankingRepository rankingRepository;
    private final MemberRepository memberRepository;

    /**
     * 인기 도시 조회
     */
    public RecommendResponse getTopCities(int limit) {
        List<City> topCities = rankingRepository.getTopCities(limit);
        if (topCities.isEmpty()) {
            return RecommendResponse.toResponse(cityRepository.findAnyCities(PageRequest.of(0, limit)));

        }
        return RecommendResponse.toResponse(topCities);
    }

    public Long getCityIdByUnique(CityUniqueDto cityUniqueDto) {
        City city = cityRepository.findByUnique(cityUniqueDto.cityName(),
                                                cityUniqueDto.countryCode(),
                                                cityUniqueDto.lat(),
                                                cityUniqueDto.lng())
            .orElseThrow(() -> new IllegalArgumentException("해당 도시를 찾을 수 없습니다."));
        return city.getCityId();
    }

    // 멤버 여부는 그냥 받기만 할게요 일단은
    public CityResponse addCity(Long memberId, CityRequest cityRequest) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        return CityResponse.toResponse(cityRepository.save(cityRequest.toCity()));
    }

    public CityResponse updateCity(Long memberId, Long cityId, CityRequest cityRequest) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        City city = cityRepository.findById(cityId)
            .orElseThrow(() -> new IllegalArgumentException("해당 도시를 찾을 수 없습니다."));

        return CityResponse.toResponse(
            city.updateCity(
                cityRequest.cityName(),
                cityRequest.countryName(),
                cityRequest.lat(),
                cityRequest.lng(),
                cityRequest.countryCode()
            ));
    }

    public CityResponse deleteCity(Long memberId, Long cityId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        return cityRepository.findById(cityId)
            .map(city -> {
                cityRepository.delete(city);
                return CityResponse.toResponse(city);
            })
            .orElseThrow(() -> new IllegalArgumentException("해당 도시를 찾을 수 없습니다."));
    }

    public CityResponse findCityByCountryAndCityName(String countryCode, String cityName) {
        return cityRepository.findByCountryCodeAndCityName(countryCode, cityName).map(CityResponse::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("해당 도시를 찾을 수 없습니다."));

    }
}
