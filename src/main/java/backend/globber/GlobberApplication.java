package backend.globber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableRedisRepositories(basePackages = "backend.globber.city.repository")
public class GlobberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobberApplication.class, args);
    }

}
