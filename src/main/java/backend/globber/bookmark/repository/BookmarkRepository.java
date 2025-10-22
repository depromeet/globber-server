package backend.globber.bookmark.repository;

import backend.globber.bookmark.domain.Bookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMember_IdAndTargetMember_Id(Long memberId, Long targetMemberId);

    List<Bookmark> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);

    List<Bookmark> findAllByMember_IdOrderByTargetMember_NameAsc(Long memberId);
}
