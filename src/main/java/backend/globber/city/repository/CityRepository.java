package backend.globber.city.repository;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.domain.City;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    @Query("SELECT c FROM City c")
    List<City> findAnyCities(Pageable pageable);

    /**
     * pg_bigm 후보군 추출 (상위 300개)
     */
    @Query(value = """
            SELECT c.city_id   AS cityId,
                   c.city_name AS cityName,
                   c.country_name AS countryName
            FROM city c
            WHERE c.city_name ILIKE '%' || :keyword || '%'
               OR c.country_name ILIKE '%' || :keyword || '%'
            LIMIT 300
            """, nativeQuery = true)
    List<SearchResponse> findCandidates(@Param("keyword") String keyword);

    @Cacheable(value = "cities", key = "#cityUniqueDto.cityName + '-' + #cityUniqueDto.countryCode + '-' + T(java.lang.String).format('%.5f', #cityUniqueDto.lat) + '-' + T(java.lang.String).format('%.5f', #cityUniqueDto.lng)")
    @Query(value = """
            SELECT c FROM City c
            WHERE c.cityName = :#{#cityUniqueDto.cityName}
              AND c.countryCode = :#{#cityUniqueDto.countryCode}
              AND c.lat = :#{#cityUniqueDto.lat}
              AND c.lng = :#{#cityUniqueDto.lng}
            """)
    Optional<City> findByCityUniqueDto(CityUniqueDto cityUniqueDto);
}
