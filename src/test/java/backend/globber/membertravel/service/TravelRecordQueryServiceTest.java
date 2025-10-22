package backend.globber.membertravel.service;

import backend.globber.support.PostgresTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import({PostgresTestConfig.class, TestDataInitializer.class})
class TravelRecordQueryServiceTest {

    @Autowired
    private TravelRecordQueryService travelRecordQueryService;

    @Autowired
    private TestDataInitializer testDataInitializer;

    @Test
    @DisplayName("회원 여행 기록 조회 - 국가별/도시별 일기 정상 매핑")
    void testGetRecordsWithDiaries() {
        // given
        Long memberId = testDataInitializer.initTravelData();

        // when
        var response = travelRecordQueryService.getRecordsWithDiaries(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalCountriesCounts()).isEqualTo(1);
        assertThat(response.totalCitiesCounts()).isEqualTo(1);
        assertThat(response.totalDiariesCounts()).isEqualTo(1);
    }
}
