package backend.globber.membertravel.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMemberTravelRequest(

    @NotBlank(message = "국가명은 필수입니다")
    String countryName,

    @NotBlank(message = "도시명은 필수입니다")
    String cityName,

    @NotNull(message = "위도는 필수입니다")
    Double lat,

    @NotNull(message = "경도는 필수입니다")
    Double lng
) {

}
