package backend.globber.membertravel.repository;

import backend.globber.city.domain.City;
import backend.globber.membertravel.domain.MemberTravelCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberTravelCityRepository extends JpaRepository<MemberTravelCity, Long> {
    List<MemberTravelCity> findByMemberTravel_Id(Long memberTravelId);

    void deleteByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);

    boolean existsByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);

    Optional<MemberTravelCity> findByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);

    @Query("""
                select distinct c
                from MemberTravelCity mtc
                join mtc.city c
                join mtc.memberTravel mt
                where mt.member.id = :memberId
            """)
    List<City> findVisitedCities(@Param("memberId") Long memberId);
}
