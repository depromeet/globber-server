package backend.globber.membertravel.domain;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import backend.globber.auth.domain.Member;
import backend.globber.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@Builder
public class MemberTravel extends BaseTimeEntity {

  @Id
  //@GeneratedValue(strategy = GenerationType.IDENTITY)
  // 일단 id는 1로 고정 -> Member 당 1개의 MemberTravel만 존재
  private final Long id = 1L;

  @OneToOne
  @JoinColumn(name = "member_id", unique = true, nullable = false)
  private Member member;

  @OneToMany(mappedBy = "memberTravel", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MemberTravelCity> memberTravelCities = new ArrayList<>();

}
