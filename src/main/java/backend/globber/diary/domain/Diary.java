package backend.globber.diary.domain;

import backend.globber.common.entity.BaseTimeEntity;
import backend.globber.membertravel.domain.MemberTravelCity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
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
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: Member_Travel_cityID -> 유저의 여행의 도시와 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_travel_city_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MemberTravelCity memberTravelCity;


    private String text;

    private String emoji;


    // 사진 리스트
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();


    public void update(String comment, String emoji) {
        this.text = comment;
        this.emoji = emoji;
    }
}
