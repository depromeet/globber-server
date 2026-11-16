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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// ProdActuatorSwaggerExposureTest와 같은 설정이라 Spring 컨텍스트를 함께 쓴다
@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({RedisTestConfig.class, PostgresTestConfig.class})
class ProdCorsPolicyTest {

    private static final String PATH = "/api/v1/profiles/me";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions preflight(String origin, String method) throws Exception {
        return mockMvc.perform(options(PATH)
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method));
    }

    @Test
    @DisplayName("허용하지 않은 Origin의 preflight는 403이고 CORS 허용 헤더를 주지 않는다")
    void unknownOrigin_rejected() throws Exception {
        // when & then
        preflight("https://evil.example", "GET")
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("운영 도메인(globber.world, www.globber.world)은 credentials와 함께 허용한다")
    void prodOrigins_allowedWithCredentials() throws Exception {
        for (String origin : new String[] {"https://globber.world", "https://www.globber.world"}) {
            // when & then
            preflight(origin, "GET")
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
    }

    @Test
    @DisplayName("운영에서는 로컬 개발 주소를 허용하지 않는다")
    void localhost_rejectedInProd() throws Exception {
        // when & then
        preflight("http://localhost:3000", "GET")
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Authorization 응답 헤더는 노출하지 않고, 쓰지 않는 메서드는 허용하지 않는다")
    void exposeHeaderAndMethodsLimited() throws Exception {
        // when & then
        preflight("https://globber.world", "GET")
                .andExpect(header().doesNotExist("Access-Control-Expose-Headers"));
        preflight("https://globber.world", "TRACE")
                .andExpect(status().isForbidden());
    }
}
