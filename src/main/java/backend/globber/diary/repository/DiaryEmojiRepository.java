package backend.globber.diary.repository;

import backend.globber.diary.domain.DiaryEmoji;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryEmojiRepository extends JpaRepository<DiaryEmoji, Long> {

    Optional<DiaryEmoji> findByDiaryIdAndCode(Long diaryId, String code);

    List<DiaryEmoji> findAllByDiaryIdOrderByCountDescCreatedAtAsc(Long diaryId);

    /**
     * 이모지 종류를 추가한다. 이미 있으면 아무것도 하지 않는다(count는 반응 기록을 통해서만 바뀐다).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO diary_emoji (diary_id, code, glyph, count, created_at, updated_at)
        VALUES (:diaryId, :code, :glyph, 0, now(), now())
        ON CONFLICT (diary_id, code) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("diaryId") Long diaryId, @Param("code") String code, @Param("glyph") String glyph);

    // Atomic Update
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DiaryEmoji e SET e.count = e.count + 1 WHERE e.diary.id = :diaryId AND e.code = :code")
    int incrementCount(@Param("diaryId") Long diaryId, @Param("code") String code);

    // 0 미만으로는 내려가지 않는다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DiaryEmoji e SET e.count = e.count - 1 WHERE e.diary.id = :diaryId AND e.code = :code AND e.count > 0")
    int decrementCount(@Param("diaryId") Long diaryId, @Param("code") String code);
}
