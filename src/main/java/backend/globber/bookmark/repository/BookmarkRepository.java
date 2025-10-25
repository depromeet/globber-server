package backend.globber.bookmark.repository;

import backend.globber.bookmark.domain.Bookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);

    List<Bookmark> findAllByMember_IdOrderByTargetMember_NameAsc(Long memberId);

    @Modifying
    int deleteByMember_IdAndTargetMember_Id(Long memberId, Long targetMemberId);
}
