package backend.globber.city.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.controller.dto.SearchResult;
import backend.globber.city.repository.CityRepository;
import backend.globber.city.repository.cache.RankingRepository;
import backend.globber.city.repository.cache.SearchRepository;
import backend.globber.exception.spec.InvalidSearchKeywordException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchRepository cacheRepository;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private SearchService searchService;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("검색어가 없거나 공백뿐이면 예외가 발생하고 캐시와 DB를 조회하지 않는다")
    void search_blankKeyword_throwsException(String keyword) {
        // given
        // when & then
        assertThatThrownBy(() -> searchService.search(keyword))
            .isInstanceOf(InvalidSearchKeywordException.class)
            .hasMessage("검색어를 입력해 주세요.");
        verifyNoInteractions(cacheRepository, rankingRepository, cityRepository);
    }

    @Test
    @DisplayName("검색어의 앞뒤 공백을 제거한 값으로 캐시 키와 후보 조회를 수행한다")
    void search_stripsKeyword() {
        // given
        given(cacheRepository.get("search:파리")).willReturn(null);
        given(cityRepository.findCandidates("파리", null)).willReturn(responses("파리", 1));
        given(rankingRepository.getScores(anyList())).willReturn(Map.of());

        // when
        SearchResult result = searchService.search("  파리  ");

        // then
        assertThat(result.cities()).extracting(SearchResponse::cityName).containsExactly("파리1");
        verify(cacheRepository).set(eq("search:파리"), any(SearchResult.class), eq(10));
    }

    @Test
    @DisplayName("검색 결과는 최대 20개까지만 반환하고 캐시에 저장한다")
    void search_limitsResultsTo20() {
        // given
        given(cacheRepository.get("search:도시")).willReturn(null);
        given(cityRepository.findCandidates("도시", null)).willReturn(responses("도시", 30));
        given(rankingRepository.getScores(anyList())).willReturn(Map.of());

        // when
        SearchResult result = searchService.search("도시");

        // then
        assertThat(result.cities()).hasSize(SearchService.MAX_SEARCH_RESULTS);
        verify(cacheRepository).set("search:도시", result, 10);
    }

    @Test
    @DisplayName("캐시에 20개를 넘는 결과가 남아 있으면 20개로 잘라 반환하고 DB를 조회하지 않는다")
    void search_cacheHit_truncatesTo20() {
        // given
        SearchResult cached = new SearchResult(responses("도시", 100));
        given(cacheRepository.get("search:도시")).willReturn(cached);

        // when
        SearchResult result = searchService.search(" 도시 ");

        // then
        assertThat(result.cities()).hasSize(SearchService.MAX_SEARCH_RESULTS);
        assertThat(result.cities()).containsExactlyElementsOf(cached.cities().subList(0, 20));
        verify(cityRepository, never()).findCandidates(anyString(), any());
        verify(cacheRepository, never()).set(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("캐시에 20개 이하의 결과가 있으면 그대로 반환한다")
    void search_cacheHit_returnsAsIs() {
        // given
        SearchResult cached = new SearchResult(responses("도시", 3));
        given(cacheRepository.get("search:도시")).willReturn(cached);

        // when
        SearchResult result = searchService.search("도시");

        // then
        assertThat(result).isSameAs(cached);
    }

    private List<SearchResponse> responses(String prefix, int count) {
        return IntStream.rangeClosed(1, count)
            .mapToObj(i -> SearchResponse.builder()
                .cityId((long) i)
                .cityName(prefix + i)
                .countryName("국가")
                .lat(0.0)
                .lng(0.0)
                .countryCode("TST")
                .build())
            .toList();
    }
}
