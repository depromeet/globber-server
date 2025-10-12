package backend.globber.diary.repository;

import backend.globber.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Long findMemberIdById(Long diaryId);

}
