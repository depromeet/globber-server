package backend.globber.membertravel.service;

import backend.globber.membertravel.controller.dto.CityCoordinates;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MockGeocodingService implements GeocodingService {

  private static final Map<String, CityCoordinates> MOCK_DATA = Map.of(
      "피렌체,이탈리아", new CityCoordinates(43.7696, 11.2558),
      "로마,이탈리아", new CityCoordinates(41.9028, 12.4964),
      "파리,프랑스", new CityCoordinates(48.8566, 2.3522),
      "런던,영국", new CityCoordinates(51.5074, -0.1278),

      // 같은 이름의 다른 도시들
      "파리,미국", new CityCoordinates(33.6617, -95.5555),  // 텍사스주 파리
      "런던,캐나다", new CityCoordinates(42.9849, -81.2453), // 온타리오주 런던
      "로마,미국", new CityCoordinates(34.2570, -85.1647)   // 조지아주 로마
  );

  @Override
  public CityCoordinates getCoordinates(String cityName, String countryName) {
    String key = cityName + "," + countryName;
    return MOCK_DATA.get(key);
  }
}
