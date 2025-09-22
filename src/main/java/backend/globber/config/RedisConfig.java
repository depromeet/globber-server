package backend.globber.config;


import backend.globber.auth.domain.RefreshToken;
import backend.globber.city.controller.dto.RecommendResponse;
import backend.globber.city.controller.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    public String REDIS_HOST;
    @Value("${spring.data.redis.port}")
    public int REDIS_PORT;

    // 레디스 연결 설정
    @Bean
    public RedisConnectionFactory redisConnectionFactory1() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(REDIS_HOST,
            REDIS_PORT);
        redisConfig.setDatabase(0);
        return new LettuceConnectionFactory(redisConfig,
            LettuceClientConfiguration.defaultConfiguration());
    }

    // 레디스 데이터 템플릿 설정, Refresh 토큰 저장용으로 String : Object(RedisToken) 형식으로 설정
    @Bean(name = "redisTemplate1")
    public RedisTemplate<String, RefreshToken> redisTemplate1() {
        RedisTemplate<String, RefreshToken> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory1());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(
            new Jackson2JsonRedisSerializer<RefreshToken>(RefreshToken.class));  // 객체를 Json 형식으로 저장
        return redisTemplate;
    }

    @Bean(name = "redisTemplate2")
    public RedisTemplate<String, SearchResult> redisTemplate2() {
        RedisTemplate<String, SearchResult> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory1());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SearchResult.class));
        return template;
    }

    @Bean(name = "redisTemplate3")
    public RedisTemplate<String, RecommendResponse> redisTemplate3() {
        RedisTemplate<String, RecommendResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory1());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(RecommendResponse.class));
        return template;
    }
}
