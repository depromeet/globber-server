package backend.globber.city.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
        }
)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cityId;

    @Column(nullable = false, length = 50)
    private String cityName;

    @Column(nullable = false, length = 50)
    private String countryName;
}
