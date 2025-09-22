package backend.globber.city.repository;

import backend.globber.city.controller.dto.SearchResponse;
import backend.globber.city.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {

    @Query("SELECT c FROM City c ORDER BY c.cityName ASC")
    List<City> findRecommended();

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
}
