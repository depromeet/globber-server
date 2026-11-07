package backend.globber.city.service;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.city.repository.cache.SearchRepository;
import backend.globber.exception.spec.InvalidSearchKeywordException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    static final int MAX_SEARCH_RESULTS = 20;

    private final SearchRepository cacheRepository;
    private final RankingRepository rankingRepository;
    private final CityRepository cityRepository;

    public SearchResult search(final String rawKeyword) {
        String keyword = normalizeKeyword(rawKeyword);
        String cacheKey = "search:" + keyword;

        SearchResult cached = cacheRepository.get(cacheKey);
        if (cached != null) {
            return limitResult(cached);
        }

        String countryCode = CountryCodeMapper.toCountryCode(keyword).orElse(null);
        List<SearchResponse> candidates = cityRepository.findCandidates(keyword, countryCode);

        Map<Long, Double> scores = rankingRepository.getScores(
                candidates.stream().map(SearchResponse::toEntity).toList()
        );

        LevenshteinDistance levenshtein = new LevenshteinDistance();

        List<SearchResponse> sorted = candidates.stream()
                .sorted(
                        Comparator
                                .comparingInt((SearchResponse c) -> exactMatchRank(c, keyword, countryCode))
                                .thenComparingInt(c -> similarityRank(c, keyword, countryCode, levenshtein))
                                .thenComparing(
                                        Comparator.comparingDouble((SearchResponse c) -> popularityRank(c, scores))
                                                .reversed()
                                )
                )
                .limit(MAX_SEARCH_RESULTS)
                .toList();

        SearchResult result = new SearchResult(sorted);
        cacheRepository.set(cacheKey, result, 10);

        return result;
    }

    private String normalizeKeyword(final String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidSearchKeywordException("검색어를 입력해 주세요.");
        }
        return keyword.strip();
    }

    // 배포 이전에 저장된 캐시에는 제한보다 많은 결과가 남아 있을 수 있다.
    private SearchResult limitResult(final SearchResult result) {
        if (result.cities() == null || result.cities().size() <= MAX_SEARCH_RESULTS) {
            return result;
        }
        return new SearchResult(List.copyOf(result.cities().subList(0, MAX_SEARCH_RESULTS)));
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


    private int exactMatchRank(final SearchResponse c, final String keyword, final String countryCode) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        if (countryCode != null && countryCode.equals(c.countryCode())) {
            return 0;
        }
        return 1;
    }

    private int similarityRank(final SearchResponse c, final String keyword, final String countryCode,
                                final LevenshteinDistance levenshtein) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        if (countryCode != null && countryCode.equals(c.countryCode())) {
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

