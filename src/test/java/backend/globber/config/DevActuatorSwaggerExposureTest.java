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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class DevActuatorSwaggerExposureTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @MockitoBean(name = "initTestAccounts")
    private CommandLineRunner initTestAccounts;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("개발 프로필에서는 API 명세(/v3/api-docs)를 그대로 노출한다")
    void apiDocsIsExposed() throws Exception {
        // given
        String path = "/v3/api-docs";

        // when & then
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("개발 프로필 설정이 노출 목록을 넓혀도 Actuator info 엔드포인트는 열리지 않는다")
    void actuatorInfoIsNotExposed() throws Exception {
        // given
        String path = "/actuator/info";

        // when & then
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("개발 프로필에서 Actuator health는 상세 정보 없이 UP을 반환한다")
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
