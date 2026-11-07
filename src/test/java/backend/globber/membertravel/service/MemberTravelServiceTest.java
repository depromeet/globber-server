package backend.globber.membertravel.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.globber.exception.spec.TravelNotFoundException;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MemberTravelServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CITY_ID = 100L;

    @Mock
    private MemberTravelRepository memberTravelRepository;

    @Mock
    private MemberTravelCityRepository memberTravelCityRepository;

    @InjectMocks
    private MemberTravelService memberTravelService;

    @Test
    @DisplayName("내 여행지에 있는 도시를 단일 삭제 쿼리로 삭제한다")
    void deleteTravelRecord_success() {
        // given
        given(memberTravelCityRepository.deleteByMemberIdAndCityId(MEMBER_ID, CITY_ID)).willReturn(1);

        // when & then
        assertThatCode(() -> memberTravelService.deleteTravelRecord(MEMBER_ID, CITY_ID))
                .doesNotThrowAnyException();
        verify(memberTravelCityRepository).deleteByMemberIdAndCityId(MEMBER_ID, CITY_ID);
        verifyNoInteractions(memberTravelRepository);
    }

    @Test
    @DisplayName("삭제된 행이 없으면 404 예외가 발생한다")
    void deleteTravelRecord_nothingDeleted() {
        // given
        given(memberTravelCityRepository.deleteByMemberIdAndCityId(MEMBER_ID, CITY_ID)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> memberTravelService.deleteTravelRecord(MEMBER_ID, CITY_ID))
                .isInstanceOf(TravelNotFoundException.class)
                .hasMessage("내 여행지에 해당 도시가 없습니다.")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberTravelCityRepository).deleteByMemberIdAndCityId(MEMBER_ID, CITY_ID);
        verifyNoInteractions(memberTravelRepository);
    }
}
