package backend.globber.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

// DevActuatorSwaggerExposureTest와 같은 설정이라 Spring 컨텍스트를 함께 쓴다
@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class DevCorsPolicyTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @MockitoBean(name = "initTestAccounts")
    private CommandLineRunner initTestAccounts;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("개발 프로필은 oauth.redirect.allowed 목록(로컬 개발 주소 포함)을 허용하고, 그 밖의 Origin은 거절한다")
    void devOrigins() throws Exception {
        // when & then
        mockMvc.perform(options("/api/v1/profiles/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mockMvc.perform(options("/api/v1/profiles/me")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
