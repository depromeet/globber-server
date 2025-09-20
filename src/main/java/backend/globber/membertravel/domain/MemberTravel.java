package backend.globber.membertravel.domain;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import backend.globber.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Pattern;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long memberId;

  @Column(nullable = false, length = 3)
  @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 3166-1 Alpha-3 형식이어야 합니다 (예: KOR, USA, JPN)")
  private String countryCode;

  private String cityName;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lng;
}
