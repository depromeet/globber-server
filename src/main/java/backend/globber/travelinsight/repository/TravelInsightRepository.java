package backend.globber.travelinsight.repository;

import backend.globber.travelinsight.domain.TravelInsight;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelInsightRepository extends JpaRepository<TravelInsight, Long> {

    Optional<TravelInsight> findByMemberId(long memberId);
}
