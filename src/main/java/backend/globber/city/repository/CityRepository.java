package backend.globber.city.repository;

import backend.globber.city.controller.dto.CityUniqueDto;
import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.domain.City;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
                   c.country_name AS countryName,
                   c.lat AS lat,
                   c.lng AS lng,
                   c.country_code AS countryCode
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

    /**
     * UPSERT (중복이면 무시)
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO city (city_name, country_name, lat, lng, country_code)
            VALUES (:cityName, :countryName, :lat, :lng, :countryCode)
            ON CONFLICT (country_code, city_name) DO NOTHING
            """, nativeQuery = true)
    void upsertCity(
            @Param("cityName") String cityName,
            @Param("countryName") String countryName,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("countryCode") String countryCode
    );

    /**
     * 국가명 + 도시명으로 조회
     */
    Optional<City> findByCountryNameAndCityName(String countryName, String cityName);
    @Query("SELECT c FROM City c WHERE c.cityName = :cityName AND c.countryCode = :countryCode AND c.lat = :lat AND c.lng = :lng")
    Optional<City> findByUnique(@Param("cityName") String cityName,
                                @Param("countryCode") String countryCode,
                                @Param("lat") Double lat,
                                @Param("lng") Double lng);

    Optional<City> findByCountryCodeAndCityName(String countryCode, String cityName);
}