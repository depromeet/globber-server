package backend.globber.travelinsight.service;

import backend.globber.membertravel.controller.dto.response.MemberTravelAllResponse;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelRepository;
import backend.globber.travelinsight.client.AiClient;
import backend.globber.travelinsight.controller.dto.response.TravelInsightResponse;
import backend.globber.travelinsight.domain.TravelInsight;
import backend.globber.travelinsight.repository.TravelInsightRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class TravelInsightServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private MemberTravelRepository memberTravelRepository;

    @Mock
    private TravelInsightRepository travelInsightRepository;

    @InjectMocks
    private TravelInsightService travelInsightService;

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

        MemberTravel memberTravel = createMemberTravel(travelUpdatedAt);

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.of(savedInsight));
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("아시아 탐험가");
        then(aiClient).shouldHaveNoInteractions();
        then(travelInsightRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("기존 인사이트가 없으면 새로 생성한다")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void shouldCreateNew_WhenNoExisting() {
        // given
        Long memberId = 1L;
        LocalDateTime travelUpdatedAt = LocalDateTime.now();
        MemberTravel memberTravel = createMemberTravel(travelUpdatedAt);

        TravelInsightResponse aiResponse = TravelInsightResponse.builder()
                .title("세계 탐험가")
                .build();

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.empty());
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(aiClient.createTitle(any(MemberTravelAllResponse.class))).willReturn(aiResponse);

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("세계 탐험가");
        then(aiClient).should().createTitle(any(MemberTravelAllResponse.class));
        then(travelInsightRepository).should().save(argThat(insight ->
                insight.getMemberId().equals(memberId) &&
                        insight.getTitle().equals("세계 탐험가")
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

        TravelInsightResponse aiResponse = TravelInsightResponse.builder()
                .title("새로운 탐험가")
                .build();

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.of(savedInsight));
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(aiClient.createTitle(any(MemberTravelAllResponse.class))).willReturn(aiResponse);

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("새로운 탐험가");
        then(savedInsight).should().updateTitle("새로운 탐험가");
        then(aiClient).should().createTitle(any(MemberTravelAllResponse.class));
        // save는 호출되지 않음 (기존 엔티티 업데이트이므로 dirty checking)
    }

    @Test
    @DisplayName("AI 호출 실패시 기본 타이틀을 반환한다")
    void shouldReturnDefault_WhenAiFails() {
        // given
        Long memberId = 1L;
        MemberTravel memberTravel = mock(MemberTravel.class);

        // ✅ 명확하게 모든 stub 설정
        given(travelInsightRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());
        given(memberTravelRepository.findAllByMember_Id(memberId))
                .willReturn(List.of(memberTravel));
        given(aiClient.createTitle(any(MemberTravelAllResponse.class)))
                .willThrow(new RuntimeException("AI 호출 실패"));

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("자유로운 여행자");
        then(travelInsightRepository).should(never()).save(any());
        then(aiClient).should().createTitle(any(MemberTravelAllResponse.class));
    }

    private MemberTravel createMemberTravel(LocalDateTime updatedAt) {
        MemberTravel memberTravel = mock(MemberTravel.class);
        given(memberTravel.getUpdatedAt()).willReturn(updatedAt);
        return memberTravel;
    }

    @Test
    @DisplayName("AI가 빈 응답을 반환하면 기본 타이틀을 반환한다")
    void shouldReturnDefault_WhenAiReturnsEmpty() {
        // given
        Long memberId = 1L;
        MemberTravel memberTravel = mock(MemberTravel.class);

        given(travelInsightRepository.findByMemberId(memberId)).willReturn(Optional.empty());
        given(memberTravelRepository.findAllByMember_Id(memberId)).willReturn(List.of(memberTravel));
        given(aiClient.createTitle(any(MemberTravelAllResponse.class)))
                .willReturn(TravelInsightResponse.empty());

        // when
        TravelInsightResponse result = travelInsightService.getOrCreateInsight(memberId);

        // then
        assertThat(result.title()).isEqualTo("자유로운 여행자");
        then(travelInsightRepository).should(never()).save(any());
    }

}
