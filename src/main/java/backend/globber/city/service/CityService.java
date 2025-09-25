package backend.globber.city.service;

import backend.globber.city.controller.dto.PagedRecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final RankingRepository rankingRepository;

    /**
     * 인기 도시 조회
     */
    public PagedRecommendResponse getTopCities(Pageable pageable) {
        Page<City> topCities = rankingRepository.getTopCitiesPaging(pageable);

        if (topCities.isEmpty()) {
            Page<City> cities = cityRepository.findAll(pageable);
            return PagedRecommendResponse.fromPage(cities);
        }

        return PagedRecommendResponse.fromPage(topCities);
    }
}
