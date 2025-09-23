package backend.globber.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;

@OpenAPIDefinition(
        servers = {
                @Server(url = "https:/globber.store", description = "운영 서버"),
                @Server(url = "http://localhost:8080", description = "로컬 서버")
        })
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
        String[] packagesToScan = {"backend.globber"};
        return GroupedOpenApi.builder()
                .group("GLOBBER API")
                .pathsToMatch(paths)
                .packagesToScan(packagesToScan)
                .build();
    }
}
