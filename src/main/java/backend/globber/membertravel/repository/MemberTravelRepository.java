package backend.globber.membertravel.repository;

import backend.globber.membertravel.domain.MemberTravel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTravelRepository extends JpaRepository<MemberTravel, Long> {

  List<MemberTravel> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
