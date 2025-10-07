package backend.globber.membertravel.repository;

import backend.globber.membertravel.domain.MemberTravelCity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTravelCityRepository extends JpaRepository<MemberTravelCity, Long> {
    List<MemberTravelCity> findByMemberTravel_Id(Long memberTravelId);
    void deleteByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);
    boolean existsByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);
    Optional<MemberTravelCity> findByMemberTravel_IdAndCity_CityId(Long memberTravelId, Long cityId);


}
