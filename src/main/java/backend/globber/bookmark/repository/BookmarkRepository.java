package backend.globber.bookmark.repository;

import backend.globber.bookmark.domain.Bookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("""
        SELECT b FROM Bookmark b
        JOIN FETCH b.targetMember
        WHERE b.member.id = :memberId
        ORDER BY b.createdAt DESC
        """)
    List<Bookmark> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);

    @Query("""
        SELECT b FROM Bookmark b
        JOIN FETCH b.targetMember
        WHERE b.member.id = :memberId
        ORDER BY b.targetMember.name ASC
        """)
    List<Bookmark> findAllByMember_IdOrderByTargetMember_NameAsc(Long memberId);

    @Modifying
    int deleteByMember_IdAndTargetMember_Id(Long memberId, Long targetMemberId);
}
