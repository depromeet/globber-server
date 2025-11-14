package backend.globber.travelinsight.service;

import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.domain.constant.PhotoTag;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.service.Continent;
import backend.globber.travelinsight.domain.TravelStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelStatisticsService {

    private final MemberTravelCityRepository memberTravelCityRepository;
    private final DiaryRepository diaryRepository;

    public TravelStatistics calculate(Long memberId) {
        List<City> visitedCities = memberTravelCityRepository.findVisitedCities(memberId);
        if (visitedCities.isEmpty()) {
            return TravelStatistics.empty();
        }

        int cityCount = visitedCities.size();

        Set<String> visitedCountries = visitedCities.stream()
            .map(City::getCountryCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        int countryCount = visitedCountries.size();

        int continentCount = visitedCountries.stream()
            .map(Continent::fromCountryCode)
            .collect(Collectors.toSet())
            .size();

        Map<PhotoTag, Long> photoTagCounts = countPhotoTags(memberId);

        return TravelStatistics.builder()
            .countryCount(countryCount)
            .cityCount(cityCount)
            .continentCount(continentCount)
            .photoTagCounts(photoTagCounts)
            .build();
    }

    private Map<PhotoTag, Long> countPhotoTags(Long memberId) {
        List<Diary> diaries = diaryRepository.findAllWithPhotoByMemberId(memberId);
        if (diaries.isEmpty()) {
            return Map.of();
        }

        return diaries.stream()
            .flatMap(diary -> diary.getPhotos().stream())
            .map(Photo::getTag)
            .filter(Objects::nonNull)
            .filter(tag -> tag != PhotoTag.NONE)
            .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }
}

