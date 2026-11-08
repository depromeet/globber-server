package backend.globber.diary.repository;

import backend.globber.diary.domain.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    int countPhotosByDiaryId(Long diaryId);

    List<Photo> findAllByDiaryId(Long diaryId);

    List<Photo> findAllByDiaryIdOrderByDisplayOrderAscIdAsc(Long diaryId);

    boolean existsByPhotoCode(String photoCode);

    @Query("""
        select p.photoCode from Photo p
        join p.diary d
        join d.memberTravelCity mtc
        join mtc.memberTravel mt
        where mt.member.id = :memberId
        """)
    List<String> findPhotoCodesByMemberId(@Param("memberId") Long memberId);

}
