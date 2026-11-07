package backend.globber.diary.domain;

import backend.globber.common.entity.BaseTimeEntity;
import backend.globber.diary.domain.constant.PhotoTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.YearMonth;
import java.util.Comparator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Photo extends BaseTimeEntity {

    // 표시 순서(display_order 오름차순, 같으면 id 오름차순)
    public static final Comparator<Photo> DISPLAY_ORDER = Comparator
        .comparingInt(Photo::getDisplayOrder)
        .thenComparing(Photo::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프론트단에서 S3에 업로드 후 반환받은 키
    @Column(nullable = false, unique = true)
    private String photoCode;

    // 메타데이터 Start

    private Double lat;      // 사진 위도

    private Double lng;      // 사진 경도

    private Long width;      // 사진 너비

    private Long height;     // 사진 높이

    private YearMonth takenMonth;   // 사진이 찍힌 시각

    private String placeName; // 사진이 찍힌 장소 이름 - 사용자 추가시 사용 가능할수도...?

    // 메타데이터 End

    private PhotoTag tag; // ex. “바다”, “여행”, “음식”

    // 기록 안에서의 표시 순서 (요청 photos 배열의 인덱스, 0부터)
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // FK: Diary (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_photo_diary"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    public void updateMetadata(Double lat, Double lng, Long width, Long height,
        YearMonth takenMonth, PhotoTag tag, String placeName) {
        this.lat = lat;
        this.lng = lng;
        this.width = width;
        this.height = height;
        this.takenMonth = takenMonth;
        this.tag = tag;
        this.placeName = placeName;
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

}

