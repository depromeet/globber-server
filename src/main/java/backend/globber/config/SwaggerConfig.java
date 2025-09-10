package backend.globber.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    // 명세서 생성
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GLOBBER")
                .version("1.0")
                .description("GLOBBER API 명세서입니다."));
    }

    @Bean
    // 그룹화된 API 생성
    public GroupedOpenApi api() {
        String[] paths = {"/**"};
        String[] packagesToScan = {"backend.globber.controller"};
        return GroupedOpenApi.builder()
            .group("GLOBBER API")
            .pathsToMatch(paths)
            .packagesToScan(packagesToScan)
            .build();
    }
}
