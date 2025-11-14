package backend.globber.diary.repository;

import backend.globber.diary.domain.Photo;
import backend.globber.diary.domain.constant.PhotoTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    int countPhotosByDiaryId(Long diaryId);

    List<Photo> findAllByDiaryId(Long diaryId);

    /**
     * 사용자의 모든 사진에서 태그별 개수 조회
     */
    @Query("""
                select p.tag, count(p)
                from Photo p
                join p.diary d
                join d.memberTravelCity mtc
                join mtc.memberTravel mt
                where mt.member.id = :memberId
                and p.tag is not null
                group by p.tag
            """)
    List<Object[]> countPhotosByTagForMember(@Param("memberId") Long memberId);

}
