package backend.globber.membertravel.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateMemberTravelRequest {

  @NotBlank(message = "국가명은 필수입니다")
  private String countryName;

  @NotBlank(message = "도시명은 필수입니다")
  private String cityName;

  @NotNull(message = "위도는 필수입니다")
  private Double lat;

  @NotNull(message = "경도는 필수입니다")
  private Double lng;
}
