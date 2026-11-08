package backend.globber.diary.repository;

import backend.globber.diary.domain.DiaryEmojiReaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryEmojiReactionRepository extends JpaRepository<DiaryEmojiReaction, Long> {

    /**
     * 반응을 추가한다. 이미 있으면 아무것도 하지 않는다(동시 요청에도 예외 없이 1번만 들어간다).
     * @return 새로 추가했으면 1, 이미 있었으면 0
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO diary_emoji_reaction (diary_id, member_id, code, created_at)
        VALUES (:diaryId, :memberId, :code, now())
        ON CONFLICT (diary_id, member_id, code) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("diaryId") Long diaryId, @Param("memberId") Long memberId, @Param("code") String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM diary_emoji_reaction
        WHERE diary_id = :diaryId AND member_id = :memberId AND code = :code
        """, nativeQuery = true)
    int deleteReaction(@Param("diaryId") Long diaryId, @Param("memberId") Long memberId, @Param("code") String code);

    @Query("select r.code from DiaryEmojiReaction r where r.diary.id = :diaryId and r.memberId = :memberId")
    List<String> findCodes(@Param("diaryId") Long diaryId, @Param("memberId") Long memberId);

    interface PressedCode {
        Long getDiaryId();

        String getCode();
    }

    @Query("""
        select r.diary.id as diaryId, r.code as code
        from DiaryEmojiReaction r
        where r.memberId = :memberId and r.diary.id in :diaryIds
        """)
    List<PressedCode> findPressedCodes(@Param("memberId") Long memberId, @Param("diaryIds") Collection<Long> diaryIds);
}
