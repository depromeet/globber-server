package backend.globber.membertravel.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.globber.exception.spec.TravelNotFoundException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberTravelServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long MEMBER_TRAVEL_ID = 10L;
    private static final Long CITY_ID = 100L;

    @Mock
    private MemberTravelRepository memberTravelRepository;

    @Mock
    private MemberTravelCityRepository memberTravelCityRepository;

    @InjectMocks
    private MemberTravelService memberTravelService;

    private MemberTravel memberTravel;

    @BeforeEach
    void setUp() {
        memberTravel = MemberTravel.builder().build();
        ReflectionTestUtils.setField(memberTravel, "id", MEMBER_TRAVEL_ID);
    }

    @Test
    @DisplayName("내 여행지에 있는 도시를 삭제한다")
    void deleteTravelRecord_success() {
        // given
        given(memberTravelRepository.findByMember_Id(MEMBER_ID)).willReturn(Optional.of(memberTravel));
        given(memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(MEMBER_TRAVEL_ID, CITY_ID))
                .willReturn(true);

        // when
        memberTravelService.deleteTravelRecord(MEMBER_ID, CITY_ID);

        // then
        verify(memberTravelCityRepository).deleteByMemberTravel_IdAndCity_CityId(MEMBER_TRAVEL_ID, CITY_ID);
    }

    @Test
    @DisplayName("여행 기록이 없는 회원이면 404 예외가 발생한다")
    void deleteTravelRecord_noMemberTravel() {
        // given
        given(memberTravelRepository.findByMember_Id(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberTravelService.deleteTravelRecord(MEMBER_ID, CITY_ID))
                .isInstanceOf(TravelNotFoundException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberTravelCityRepository, never()).deleteByMemberTravel_IdAndCity_CityId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내 여행지에 해당 도시가 없으면 404 예외가 발생하고 삭제하지 않는다")
    void deleteTravelRecord_cityNotInMyTravel() {
        // given
        given(memberTravelRepository.findByMember_Id(MEMBER_ID)).willReturn(Optional.of(memberTravel));
        given(memberTravelCityRepository.existsByMemberTravel_IdAndCity_CityId(MEMBER_TRAVEL_ID, CITY_ID))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberTravelService.deleteTravelRecord(MEMBER_ID, CITY_ID))
                .isInstanceOf(TravelNotFoundException.class)
                .hasMessage("내 여행지에 해당 도시가 없습니다.")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberTravelCityRepository, never()).deleteByMemberTravel_IdAndCity_CityId(anyLong(), anyLong());
    }
}
