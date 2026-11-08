package backend.globber.membertravel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.membertravel.controller.dto.CountryRecordDto;
import backend.globber.membertravel.controller.dto.response.TravelRecordWithDiaryResponse;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelRecordQueryServiceUnitTest {

    @Mock
    private MemberTravelCityRepository memberTravelCityRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @InjectMocks
    private TravelRecordQueryService travelRecordQueryService;

    @Test
    @DisplayName("기록이 없는 방문 도시도 나라 수와 도시 수에 포함한다")
    void shouldCountVisitedCities_WithoutDiary() {
        // given
        Long memberId = 1L;
        City seoul = city(1L, "서울", "대한민국", "KOR");
        City busan = city(2L, "부산", "대한민국", "KOR");
        City tokyo = city(3L, "도쿄", "일본", "JPN");
        given(memberTravelCityRepository.findVisitedCities(memberId)).willReturn(List.of(seoul, busan, tokyo));
        given(diaryRepository.findAllWithPhotoByMemberId(memberId)).willReturn(List.of(diary(tokyo)));

        // when
        TravelRecordWithDiaryResponse response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        // then
        assertThat(response.totalCountriesCounts()).isEqualTo(2);
        assertThat(response.totalCitiesCounts()).isEqualTo(3);
        assertThat(response.totalDiariesCounts()).isEqualTo(1);
        assertThat(country(response, "KOR").diaryCount()).isZero();
        assertThat(country(response, "KOR").cities()).hasSize(2);
        assertThat(country(response, "JPN").diaryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 도시에 기록이 여러 개면 나라별 diaryCount는 실제 기록 수의 합이다")
    void shouldSumDiaryCount_WhenCityHasMultipleDiaries() {
        // given
        Long memberId = 1L;
        City tokyo = city(1L, "도쿄", "일본", "JPN");
        City osaka = city(2L, "오사카", "일본", "JPN");
        City kyoto = city(3L, "교토", "일본", "JPN");
        given(memberTravelCityRepository.findVisitedCities(memberId)).willReturn(List.of(tokyo, osaka, kyoto));
        given(diaryRepository.findAllWithPhotoByMemberId(memberId)).willReturn(List.of(
                diary(tokyo), diary(tokyo), diary(tokyo), diary(osaka)
        ));

        // when
        TravelRecordWithDiaryResponse response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        // then
        assertThat(response.totalCountriesCounts()).isEqualTo(1);
        assertThat(response.totalCitiesCounts()).isEqualTo(3);
        assertThat(country(response, "JPN").diaryCount()).isEqualTo(4);
        assertThat(response.totalDiariesCounts()).isEqualTo(4);
    }

    @Test
    @DisplayName("방문 도시 목록에 없는 도시의 기록은 합계에서 제외한다")
    void shouldExcludeDiaries_OfCityNotInVisitedCities() {
        // given
        Long memberId = 1L;
        City tokyo = city(1L, "도쿄", "일본", "JPN");
        City paris = city(2L, "파리", "프랑스", "FRA");
        given(memberTravelCityRepository.findVisitedCities(memberId)).willReturn(List.of(tokyo));
        given(diaryRepository.findAllWithPhotoByMemberId(memberId)).willReturn(List.of(
                diary(tokyo), diary(paris), diary(paris)
        ));

        // when
        TravelRecordWithDiaryResponse response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        // then
        assertThat(response.totalCountriesCounts()).isEqualTo(1);
        assertThat(response.totalCitiesCounts()).isEqualTo(1);
        assertThat(country(response, "JPN").diaryCount()).isEqualTo(1);
        assertThat(response.totalDiariesCounts()).isEqualTo(1);
    }

    private CountryRecordDto country(TravelRecordWithDiaryResponse response, String countryCode) {
        return response.records().stream()
                .filter(record -> record.countryCode().equals(countryCode))
                .findFirst()
                .orElseThrow();
    }

    private City city(Long cityId, String cityName, String countryName, String countryCode) {
        return City.builder()
                .cityId(cityId)
                .cityName(cityName)
                .countryName(countryName)
                .countryCode(countryCode)
                .lat(0.0)
                .lng(0.0)
                .build();
    }

    private Diary diary(City city) {
        return Diary.builder()
                .memberTravelCity(MemberTravelCity.builder().city(city).build())
                .text("memo")
                .build();
    }
}
