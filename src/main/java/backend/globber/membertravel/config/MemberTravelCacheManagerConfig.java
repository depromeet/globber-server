package backend.globber.membertravel.config;

import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class MemberTravelCacheManagerConfig {

    @Bean(name = "memberTravelCacheManager")
    public CacheManager MemberTravelCacheManager(
        @Qualifier("redisTemplate4") RedisTemplate<String, MemberTravelAllResponse> redisTemplate4) {

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisTemplate4.getConnectionFactory())
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(MemberTravelAllResponse.class)
                    )
                )
                .prefixCacheNameWith("cache:memberTravels:")
                .entryTtl(Duration.ofMinutes(30)) // TTL 30분
            )
            .build();
    }
}

