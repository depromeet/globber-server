package backend.globber.membertravel.service;

import backend.globber.membertravel.controller.dto.CityCoordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityCoordinatesRedisService {

  private final RedisTemplate<String, CityCoordinates> redisTemplate;
  private static final String CITY_COORDINATES_KEY_PREFIX = "city_coordinates:";

  /**
   * 도시 좌표 정보를 Redis에 저장
   */
  public void saveCityCoordinates(String cityName, String countryName, CityCoordinates coordinates) {
    try {
      String key = buildRedisKey(cityName, countryName);
      redisTemplate.opsForValue().set(key, coordinates);
      log.info("Redis에 도시 좌표 저장 완료 - Key: {}, 좌표: ({}, {})",
          key, coordinates.getLat(), coordinates.getLng());
    } catch (Exception e) {
      log.error("Redis 저장 실패 - 도시: {}, 국가: {}", cityName, countryName, e);
    }
  }

  /**
   * Redis에서 도시 좌표 정보 조회
   */
  public CityCoordinates getCityCoordinates(String cityName, String countryName) {
    try {
      String key = buildRedisKey(cityName, countryName);
      CityCoordinates result = redisTemplate.opsForValue().get(key);

      if (result != null) {
        log.info("Redis 캐시 히트 - Key: {}", key);
        return result;
      }

      log.info("Redis 캐시 미스 - Key: {}", key);
      return null;
    } catch (Exception e) {
      log.error("Redis 조회 실패 - 도시: {}, 국가: {}", cityName, countryName, e);
      return null;
    }
  }

  /**
   * Redis 키 생성
   */
  private String buildRedisKey(String cityName, String countryName) {
    return CITY_COORDINATES_KEY_PREFIX + cityName + "," + countryName;
  }
}
