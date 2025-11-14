package backend.globber.travelinsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.travelinsight.domain.TravelStatistics;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelStatisticsServiceTest {

    @Mock
    private MemberTravelCityRepository memberTravelCityRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @InjectMocks
    private TravelStatisticsService travelStatisticsService;

    @Test
    @DisplayName("방문 도시, 국가, 대륙, 사진 태그를 정확히 계산한다")
    void shouldCalculateStatistics_FromRepositories() {
        // given
        Long memberId = 1L;
        given(memberTravelCityRepository.findVisitedCities(memberId)).willReturn(List.of(
            city("Seoul", "대한민국", "KOR"),
            city("Busan", "대한민국", "KOR"),
            city("Tokyo", "일본", "JPN"),
            city("Paris", "프랑스", "FRA")
        ));

        Diary diary1 = diaryWith(PhotoTag.FOOD, PhotoTag.PEOPLE);
        Diary diary2 = diaryWith(PhotoTag.SCENERY, PhotoTag.SCENERY, PhotoTag.FOOD);
        given(diaryRepository.findAllWithPhotoByMemberId(memberId)).willReturn(List.of(diary1, diary2));

        // when
        TravelStatistics statistics = travelStatisticsService.calculate(memberId);

        // then
        assertThat(statistics.getCityCount()).isEqualTo(4);
        assertThat(statistics.getCountryCount()).isEqualTo(3);
        assertThat(statistics.getContinentCount()).isEqualTo(2);
        assertThat(statistics.getPhotoTagCounts())
            .containsEntry(PhotoTag.FOOD, 2L)
            .containsEntry(PhotoTag.PEOPLE, 1L)
            .containsEntry(PhotoTag.SCENERY, 2L);
    }

    @Test
    @DisplayName("방문 도시가 없으면 기본 통계를 반환한다")
    void shouldReturnEmpty_WhenNoVisitedCity() {
        // given
        Long memberId = 2L;
        given(memberTravelCityRepository.findVisitedCities(memberId)).willReturn(List.of());

        // when
        TravelStatistics statistics = travelStatisticsService.calculate(memberId);

        // then
        assertThat(statistics).usingRecursiveComparison()
            .isEqualTo(TravelStatistics.empty());
        verify(diaryRepository, never()).findAllWithPhotoByMemberId(memberId);
    }

    private City city(String cityName, String countryName, String countryCode) {
        return City.builder()
            .cityName(cityName)
            .countryName(countryName)
            .countryCode(countryCode)
            .lat(0.0)
            .lng(0.0)
            .build();
    }

    private Diary diaryWith(PhotoTag... tags) {
        Diary diary = Diary.builder()
            .memberTravelCity(null)
            .text("memo")
            .build();

        for (PhotoTag tag : tags) {
            diary.getPhotos().add(Photo.builder()
                .photoCode(UUID.randomUUID().toString())
                .takenMonth(YearMonth.now())
                .tag(tag)
                .diary(diary)
                .build());
        }
        return diary;
    }
}

