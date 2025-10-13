package backend.globber.diary.repository;

import backend.globber.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Long findMemberIdById(Long diaryId);

    @Query("""
                select distinct d
                from Diary d
                join fetch d.memberTravelCity mtc
                join fetch mtc.city c
                left join fetch d.photos p
                where mtc.memberTravel.member.id = :memberId
            """)
    List<Diary> findAllWithPhotoByMemberId(@Param("memberId") Long memberId);
}


