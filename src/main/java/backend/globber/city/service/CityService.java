package backend.globber.city.service;

import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RecommendedCityListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final RecommendedCityListRepository recommendedCityListRepository;

    private static final String KEY = "recommended:city:list";

    public RecommendResponse getRecommendedCities() {
        RecommendResponse cached = recommendedCityListRepository.getRecommendedCities(KEY);
        if (cached != null) {
            return cached;
        }

        List<City> cities = cityRepository.findRecommended();

        recommendedCityListRepository.saveRecommendedCities(KEY, cities);

        return RecommendResponse.toResponse(cities);
    }
}