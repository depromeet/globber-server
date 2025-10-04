package backend.globber.membertravel.controller.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMemberTravelRequest(

    @NotBlank(message = "국가명은 필수입니다")
    @Size(max = 100, message = "국가명은 최대 100자까지 가능합니다")
    String countryName,

    @NotBlank(message = "도시명은 필수입니다")
    @Size(max = 100, message = "도시명은 최대 100자까지 가능합니다")
    String cityName,

    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
    Double lat,

    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
    Double lng,

    @NotBlank(message = "국가 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 3166-1 Alpha-3 형식이어야 합니다 (예: KOR, USA, JPN)")
    String countryCode
) {

}
