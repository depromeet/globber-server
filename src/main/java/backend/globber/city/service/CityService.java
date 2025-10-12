package backend.globber.city.service;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final RankingRepository rankingRepository;

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
}
