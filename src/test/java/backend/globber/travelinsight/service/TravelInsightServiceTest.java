package backend.globber.travelinsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.client.AiClient;
import backend.globber.travelinsight.config.TravelInsightSettings;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.domain.TravelStatistics;
import backend.globber.travelinsight.domain.TravelTitleComposer;
import backend.globber.travelinsight.domain.constant.TitleStrategy;
import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TravelInsightServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private MemberTravelRepository memberTravelRepository;

    @Mock
    private TravelInsightRepository travelInsightRepository;

    @Mock
    private TravelStatisticsService travelStatisticsService;

    @Mock
    private TravelInsightSettings travelInsightSettings;

    @Spy
    private TravelTitleComposer travelTitleComposer = new TravelTitleComposer();

    @InjectMocks
    private TravelInsightService travelInsightService;

    @BeforeEach
    void setUp() {
        lenient().when(travelInsightSettings.getTitleStrategy()).thenReturn(TitleStrategy.SERVER);
    }

    @Test
    @DisplayName("캐시가 유효하면 기존 인사이트를 반환한다")
    void shouldReturnCached_WhenCacheValid() {
        // given
        Long memberId = 1L;
        LocalDateTime insightUpdatedAt = LocalDateTime.now();
        LocalDateTime travelUpdatedAt = insightUpdatedAt.minusHours(1); // 인사이트가 더 최신

        TravelInsight savedInsight = TravelInsight.builder()
            .memberId(memberId)
            .title("아시아 탐험가")
            .build();
        ReflectionTestUtils.setField(savedInsight, "updatedAt", insightUpdatedAt);

    @Spy
    private TravelTitleComposer travelTitleComposer = new TravelTitleComposer();

    @InjectMocks
    private TravelInsightService travelInsightService;

    @BeforeEach
    void setUp() {
        lenient().when(travelInsightSettings.getTitleStrategy()).thenReturn(TitleStrategy.SERVER);
    }

    @Test
    @DisplayName("기존 인사이트가 없으면 새로 생성한다")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void shouldCreateNew_WhenNoExisting() {
        // given
        Long memberId = 1L;
        LocalDateTime travelUpdatedAt = LocalDateTime.now();
        MemberTravel memberTravel = createMemberTravel(travelUpdatedAt);

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.empty());
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(travelStatisticsService.calculate(memberId)).willReturn(defaultStatistics());

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo(expectedServerTitle());
        then(aiClient).shouldHaveNoInteractions();
        then(travelInsightRepository).should().save(argThat(insight ->
            insight.getMemberId().equals(memberId) &&
                insight.getTitle().equals(expectedServerTitle())
        ));
    }

    @Test
    @DisplayName("여행 데이터가 변경되면 인사이트를 업데이트한다")
    void shouldUpdate_WhenTravelDataChanged() {
        // given
        Long memberId = 1L;
        LocalDateTime insightUpdatedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime travelUpdatedAt = LocalDateTime.now(); // 여행 데이터가 더 최신

        TravelInsight savedInsight = spy(TravelInsight.builder()
            .memberId(memberId)
            .title("기존 탐험가")
            .build());
        ReflectionTestUtils.setField(savedInsight, "updatedAt", insightUpdatedAt);

        MemberTravel memberTravel = createMemberTravel(travelUpdatedAt);

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.of(savedInsight));
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(travelStatisticsService.calculate(memberId)).willReturn(defaultStatistics());

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo(expectedServerTitle());
        then(savedInsight).should().updateTitle(expectedServerTitle());
        then(aiClient).shouldHaveNoInteractions();
        // save는 호출되지 않음 (기존 엔티티 업데이트이므로 dirty checking)
    }

    private MemberTravel createMemberTravel(LocalDateTime updatedAt) {
        MemberTravel memberTravel = mock(MemberTravel.class);
        lenient().when(memberTravel.getUpdatedAt()).thenReturn(updatedAt);
        return memberTravel;
    }

    @Test
    @DisplayName("여행 통계가 없으면 AI 호출 없이 기본 타이틀을 반환한다")
    void shouldReturnDefault_WhenStatisticsEmpty() {
        // given
        Long memberId = 1L;
        MemberTravel memberTravel = createMemberTravel(LocalDateTime.now());

        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(travelStatisticsService.calculate(memberId)).willReturn(TravelStatistics.empty());

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("자유로운 여행자");
        then(aiClient).shouldHaveNoInteractions();
    }

    private TravelStatistics defaultStatistics() {
        return TravelStatistics.builder()
            .countryCount(5)
            .cityCount(10)
            .continentCount(2)
            .photoTagCounts(Map.of())
            .build();
    }

    private String expectedServerTitle() {
        TravelStatistics statistics = defaultStatistics();
        TravelLevel level = TravelLevel.determineLevel(statistics.getCountryCount(), statistics.getCityCount());
        TravelScope scope = TravelScope.determineScope(statistics.getCountryCount(), statistics.getContinentCount());
        TravelType type = TravelType.determineType(level, statistics.getPhotoTagCounts());
        return travelTitleComposer.compose(level, scope, type);
    }
}
