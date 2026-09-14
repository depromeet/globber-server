package backend.globber.city.controller;

import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.service.CityService;
import backend.globber.city.service.SearchService;
import backend.globber.common.service.CommonService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CityControllerAllTest {

    @Mock private CityService cityService;
    @Mock private SearchService searchService;
    @Mock private CommonService commonService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
            new CityController(cityService, searchService, commonService)).build();
    }

    @Test
    void allReturnsCitiesWithoutSearchKeyword() throws Exception {
        when(cityService.getAllCities()).thenReturn(List.of(
            new CityResponse(1L, "지베르니", "프랑스", 49.0765, 1.5308, "FRA"),
            new CityResponse(2L, "컨딩", "대만", 21.9468, 120.7984, "TWN")
        ));

        mvc.perform(get("/api/v1/cities/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].cityName").value("지베르니"))
            .andExpect(jsonPath("$.data[1].countryCode").value("TWN"));
    }
}
