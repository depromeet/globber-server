package backend.globber.diary.repository;

import backend.globber.diary.domain.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    int countPhotosByDiaryId(Long diaryId);

    List<Photo> findAllByDiaryId(Long diaryId);

    List<Photo> findAllByDiaryIdOrderByCreatedAtAsc(Long diaryId);

}
