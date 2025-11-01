package backend.globber.diary.repository;

import backend.globber.diary.domain.DiaryEmoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiaryEmojiRepository extends JpaRepository<DiaryEmoji, Long> {

    Optional<DiaryEmoji> findByDiaryIdAndCode(Long diaryId, String code);

    List<DiaryEmoji> findAllByDiaryIdOrderByCountDescCreatedAtAsc(Long diaryId);

    // Atomic Update
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DiaryEmoji e SET e.count = e.count + 1 WHERE e.diary.id = :diaryId AND e.code = :code")
    int incrementCount(@Param("diaryId") Long diaryId, @Param("code") String code);
}
