package backend.globber.membertravel.service;

import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.membertravel.controller.dto.CityRecordDto;
import backend.globber.membertravel.controller.dto.CountryRecordDto;
import backend.globber.membertravel.controller.dto.response.TravelRecordWithDiaryResponse;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelRecordQueryService {

    private final MemberTravelCityRepository memberTravelCityRepository;
    private final DiaryRepository diaryRepository;

    public TravelRecordWithDiaryResponse getRecordsWithDiaries(Long memberId) {

        List<City> visitedCities = memberTravelCityRepository.findVisitedCities(memberId);
        List<Diary> diaries = diaryRepository.findAllWithPhotoByMemberId(memberId);

        Map<Long, List<Diary>> cityToDiaries = getCityToDiaries(diaries);

        Map<String, List<City>> citiesByCountry = visitedCities.stream()
                .collect(Collectors.groupingBy(City::getCountryCode));

        List<CountryRecordDto> records = citiesByCountry.values().stream()
                .map(cities -> {
                    List<CityRecordDto> cityDtos = cities.stream()
                            .map(city -> {
                                List<Diary> cityDiaries = cityToDiaries.getOrDefault(city.getCityId(), List.of());
                                List<String> thumbnails = getThumbnails(cityDiaries);
                                return getCityRecordDto(city, cityDiaries, thumbnails);
                            })
                            .toList();

                    City sample = cities.getFirst();
                    return getCountryRecordDto(sample, cityDtos);
                })
                .toList();

        return TravelRecordWithDiaryResponse.builder()
                .totalCountriesCounts(getTotalCountries(records))
                .totalCitiesCounts(getTotalCities(records))
                .totalDiariesCounts(diaries.size())
                .records(records)
                .build();
    }

    private static CountryRecordDto getCountryRecordDto(City sample, List<CityRecordDto> cityDtos) {
        return CountryRecordDto.builder()
                .countryName(sample.getCountryName())
                .countryCode(sample.getCountryCode())
                .continent(Continent.fromCountryCode(sample.getCountryCode()).name())
                .diaryCount(getDiaryCount(cityDtos))
                .cities(cityDtos)
                .build();
    }

    private static long getDiaryCount(List<CityRecordDto> cityDtos) {
        return cityDtos.stream().filter(CityRecordDto::hasDiary).count();
    }

    private static CityRecordDto getCityRecordDto(City city, List<Diary> cityDiaries, List<String> thumbnails) {
        return CityRecordDto.builder()
                .id(city.getCityId())
                .name(city.getCityName())
                .hasDiary(!cityDiaries.isEmpty())
                .thumbnailUrls(thumbnails)
                .build();
    }

    private static List<String> getThumbnails(List<Diary> cityDiaries) {
        return cityDiaries.stream()
                .flatMap(diary -> diary.getPhotos().stream())
                .map(Photo::getPhotoCode)
                .distinct()
                .limit(3)
                .toList();
    }

    private static Map<Long, List<Diary>> getCityToDiaries(List<Diary> diaries) {
        return diaries.stream()
                .collect(Collectors.groupingBy(d -> d.getMemberTravelCity().getCity().getCityId()));
    }

    private static long getTotalCountries(List<CountryRecordDto> records) {
        return records.stream()
                .filter(r -> r.diaryCount() > 0)
                .count();
    }

    private static long getTotalCities(List<CountryRecordDto> records) {
        return records.stream()
                .flatMap(r -> r.cities().stream())
                .filter(CityRecordDto::hasDiary)
                .count();
    }
}