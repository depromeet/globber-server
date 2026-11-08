package backend.globber.diary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 누가 어떤 기록에 어떤 이모지를 눌렀는지 기록한다. (diary_id, member_id, code)는 유일하다.
 * 쓰기는 동시성 처리를 위해 DiaryEmojiReactionRepository의 ON CONFLICT 쿼리로만 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "diary_emoji_reaction",
    uniqueConstraints = @UniqueConstraint(name = "uq_diary_emoji_reaction", columnNames = {"diary_id", "member_id", "code"}),
    indexes = @Index(name = "idx_diary_emoji_reaction_member", columnList = "member_id, diary_id")
)
public class DiaryEmojiReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
