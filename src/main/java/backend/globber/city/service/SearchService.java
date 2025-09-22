package backend.globber.city.service;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.CacheRepository;
import backend.globber.city.repository.cache.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final CacheRepository cacheRepository;
    private final RankingRepository rankingRepository;
    private final CityRepository cityRepository;

    public SearchResult search(String keyword) {
        String cacheKey = "search:" + keyword;

        SearchResult cached = cacheRepository.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<SearchResponse> candidates = cityRepository.findCandidates(keyword);

        Map<String, Double> scores = rankingRepository.getScores(
                candidates.stream().map(SearchResponse::cityName).toList()
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

    public void recordSelection(String cityName) {
        rankingRepository.incrementScore(cityName);
    }

    private int exactMatchRank(SearchResponse c, String keyword) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        return 1;
    }

    private int similarityRank(SearchResponse c, String keyword, LevenshteinDistance levenshtein) {
        if (c.cityName().equals(keyword) || c.countryName().equals(keyword)) {
            return 0;
        }
        return levenshtein.apply(c.cityName(), keyword);
    }

    private double popularityRank(SearchResponse c, Map<String, Double> scores) {
        if (scores.containsKey(c.cityName())) {
            return scores.get(c.cityName());
        }
        return 0.0;
    }
}

