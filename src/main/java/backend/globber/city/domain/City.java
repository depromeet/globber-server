package backend.globber.city.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        indexes = {
                @Index(name = "idx_city_name", columnList = "cityName"),
                @Index(name = "idx_country_name", columnList = "countryName"),
                @Index(name = "idx_country_city", columnList = "countryName, cityName")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_country_code_city",
                        columnNames = {"countryCode", "cityName"}
                )
        }
)
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cityId;

    @Column(nullable = false, length = 50)
    private String cityName;

    @Column(nullable = false, length = 50)
    private String countryName;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false, length = 3)
    @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 3166-1 Alpha-3 형식이어야 합니다 (예: KOR, USA, JPN)")
    private String countryCode;

    public City updateCity(String cityName, String countryName, Double lat, Double lng,
                           String countryCode) {
        this.cityName = cityName;
        this.countryName = countryName;
        this.lat = lat;
        this.lng = lng;
        this.countryCode = countryCode;
        return this;
    }
}
