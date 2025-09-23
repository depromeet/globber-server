package backend.globber.city.service;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.city.repository.cache.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository cacheRepository;
    private final RankingRepository rankingRepository;
    private final CityRepository cityRepository;

    public SearchResult search(final String keyword) {
        String cacheKey = "search:" + keyword;

        SearchResult cached = cacheRepository.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<SearchResponse> candidates = cityRepository.findCandidates(keyword);

        Map<Long, Double> scores = rankingRepository.getScores(
                candidates.stream().map(SearchResponse::toEntity).toList()
        );

        LevenshteinDistance levenshtein = new LevenshteinDistance();

        List<SearchResponse> sorted = candidates.stream()
                .sorted(
                        Comparator
                                .comparingInt((SearchResponse c) -> exactMatchRank(c, keyword))
                                .thenComparingInt(c -> similarityRank(c, keyword, levenshtein))
                                .thenComparing(
                                        Comparator.comparingDouble((SearchResponse c) -> popularityRank(c, scores))
                                                .reversed()
                                )
                )
                .limit(100)
                .toList();

        SearchResult result = new SearchResult(sorted);
        cacheRepository.set(cacheKey, result, 10);

        return result;
    }

    public void recordSelection(final City city) {
        rankingRepository.incrementScore(city);
    }


    public SearchResult getPopularCities(final int limit) {
        List<City> topCities = rankingRepository.getTopCities(limit);

        List<SearchResponse> responses = topCities.stream()
                .map(city -> SearchResponse.builder()
                        .cityId(city.getCityId())
                        .cityName(city.getCityName())
                        .countryName(city.getCountryName())
                        .build()
                )
                .toList();

        return new SearchResult(responses);
    }


    private int exactMatchRank(final SearchResponse c, final String keyword) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        return 1;
    }

    private int similarityRank(final SearchResponse c, final String keyword, final LevenshteinDistance levenshtein) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        return levenshtein.apply(c.cityName(), keyword);
    }

    private double popularityRank(final SearchResponse c, final Map<Long, Double> scores) {
        if (scores.containsKey(c.cityId())) {
            return scores.get(c.cityId());
        }
        return 0.0;
    }
}

