package backend.globber.bookmark.repository;

import backend.globber.bookmark.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMember_IdAndTargetMember_Id(Long memberId, Long targetMemberId);
}
