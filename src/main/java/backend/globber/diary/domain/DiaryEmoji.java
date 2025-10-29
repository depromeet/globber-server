package backend.globber.diary.domain;

import backend.globber.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_diary_emoji", columnNames = {"diary_id", "code"})
        }
)
public class DiaryEmoji extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    // 유니코드 코드포인트나 커스텀 코드
    @Column(nullable = false, length = 64)
    private String code;

    //이모지 glyph
    @Column(nullable = false, length = 8)
    private String glyph;

    @Column(nullable = false)
    @Builder.Default
    private Long count = 0L;

    public void increaseCount() {
        this.count++;
    }

    public void setDiary(Diary diary) {
        this.diary = diary;
    }
}
