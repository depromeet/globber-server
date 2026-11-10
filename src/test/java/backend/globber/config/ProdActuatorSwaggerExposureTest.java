package backend.globber.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.globber.city.service.RedisWarmUp;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class ProdActuatorSwaggerExposureTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("운영 프로필에서도 API 명세(/v3/api-docs)를 제공한다. 접근 제한은 Nginx Basic Auth가 맡는다")
    void apiDocsIsExposed() throws Exception {
        // given
        String path = "/v3/api-docs/swagger-config";

        // when & then
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("운영 프로필에서도 Swagger UI를 제공한다")
    void swaggerUiIsExposed() throws Exception {
        // given
        String indexPath = "/swagger-ui/index.html";

        // when & then
        mockMvc.perform(get(indexPath))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("운영 프로필에서는 Actuator info 엔드포인트를 노출하지 않는다")
    void actuatorInfoIsNotExposed() throws Exception {
        // given
        String path = "/actuator/info";

        // when & then
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("운영 프로필에서도 Actuator health는 상세 정보 없이 UP을 반환한다")
    void actuatorHealthIsExposedWithoutDetails() throws Exception {
        // given
        String path = "/actuator/health";

        // when & then
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
