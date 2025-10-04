package backend.globber.membertravel.repository;

import backend.globber.membertravel.domain.MemberTravel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberTravelRepository extends JpaRepository<MemberTravel, Long> {
    Optional<MemberTravel> findByMember_Id(Long memberId); // Member와 1:1 이라면 Optional

    List<MemberTravel> findAllByMember_Id(Long memberId);  // 혹시 여러개 고려할 경우

    Optional<MemberTravel> findByMember_Uuid(String uuid);  // UUID로 조회
}
