package backend.globber.diary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.DiaryEmojiReactionRepository;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.common.service.S3ObjectDeleter;
import backend.globber.common.service.UploadedImageValidator;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.diary.service.PhotoService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.NoCredException;
import backend.globber.exception.spec.PhotoNotFoundException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OwnershipChecksTest {

    private static final long OWNER_ID = 1L;
    private static final long OTHER_ID = 2L;
    private static final long DIARY_ID = 10L;
    private static final long PHOTO_ID = 20L;

    private final DiaryRepository diaryRepository = mock(DiaryRepository.class);
    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PhotoServiceImpl photoService = new PhotoServiceImpl(photoRepository,
        mock(UploadedImageValidator.class), mock(S3ObjectDeleter.class));
    private final PhotoService photoServiceMock = mock(PhotoService.class);
    private final DiaryServiceImpl diaryService = new DiaryServiceImpl(diaryRepository, photoRepository,
        memberRepository, mock(CityRepository.class), mock(JwtTokenProvider.class), photoServiceMock,
        mock(MemberTravelCityRepository.class), mock(MemberTravelRepository.class),
        mock(DiaryEmojiRepository.class), mock(DiaryEmojiReactionRepository.class), mock(S3ObjectDeleter.class));

    private Member member(long id) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));
        return member;
    }

    private Diary diaryOwnedBy(Member owner, City city) {
        MemberTravel travel = MemberTravel.builder().member(owner).build();
        MemberTravelCity memberTravelCity = MemberTravelCity.builder().memberTravel(travel).city(city).build();
        Diary diary = Diary.builder().id(DIARY_ID).memberTravelCity(memberTravelCity).text("original").build();
        ReflectionTestUtils.setField(diary, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(diary, "updatedAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        when(diaryRepository.findWithCityById(DIARY_ID)).thenReturn(Optional.of(diary));
        when(diaryRepository.findById(DIARY_ID)).thenReturn(Optional.of(diary));
        return diary;
    }

    private City city(long cityId) {
        return City.builder().cityId(cityId).cityName("서울").countryName("한국").countryCode("KOR").build();
    }

    @Test
    @DisplayName("다른 회원의 기록은 수정할 수 없다")
    void updateDiary_otherMember_throws() {
        // given
        Diary diary = diaryOwnedBy(member(OWNER_ID), city(12L));
        member(OTHER_ID);
        DiaryRequest request = new DiaryRequest(99L, "changed", List.of());

        // when & then
        assertThatThrownBy(() -> diaryService.updateDiary(OTHER_ID, DIARY_ID, request))
            .isInstanceOf(NoCredException.class);
        assertThat(diary.getText()).isEqualTo("original");
    }

    @Test
    @DisplayName("다른 회원의 기록은 삭제할 수 없다")
    void deleteDiary_otherMember_throws() {
        // given
        diaryOwnedBy(member(OWNER_ID), city(12L));
        member(OTHER_ID);

        // when & then
        assertThatThrownBy(() -> diaryService.deleteDiary(OTHER_ID, DIARY_ID))
            .isInstanceOf(NoCredException.class);
        verify(diaryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("기록 수정 요청에 다른 cityId가 있어도 기존 도시를 유지한다")
    void updateDiary_differentCityId_keepsOriginalCity() {
        // given
        Diary diary = diaryOwnedBy(member(OWNER_ID), city(12L));
        DiaryRequest request = new DiaryRequest(99L, "after", List.of());

        // when
        DiaryResponse response = diaryService.updateDiary(OWNER_ID, DIARY_ID, request);

        // then
        assertThat(response.city().cityId()).isEqualTo(12L);
        assertThat(diary.getText()).isEqualTo("after");
    }

    @Test
    @DisplayName("다른 회원의 기록은 사진 목록도 바꿀 수 없다")
    void updateDiary_otherMember_doesNotTouchPhotos() {
        // given
        diaryOwnedBy(member(OWNER_ID), city(12L));
        member(OTHER_ID);
        PhotoRequest newPhoto = new PhotoRequest(null, "new", null, null, 1L, 1L, null, null, null);
        DiaryRequest request = new DiaryRequest(12L, "changed", List.of(newPhoto));

        // when & then
        assertThatThrownBy(() -> diaryService.updateDiary(OTHER_ID, DIARY_ID, request))
            .isInstanceOf(NoCredException.class);
        verify(photoServiceMock, never()).syncPhotos(any(), any(), any());
    }

    @Test
    @DisplayName("본인 기록이어도 다른 기록의 사진은 수정·삭제할 수 없다")
    void syncPhotos_photoFromAnotherDiary_throws() {
        // given
        Diary diary = Diary.builder().id(DIARY_ID).build();
        PhotoRequest otherDiaryPhoto = new PhotoRequest(PHOTO_ID, "other", null, null, 1L, 1L, null, null, null);

        // when & then
        assertThatThrownBy(() -> photoService.syncPhotos(OWNER_ID, diary, List.of(otherDiaryPhoto)))
            .isInstanceOf(PhotoNotFoundException.class);
        verify(photoRepository, never()).save(any());
        verify(photoRepository, never()).delete(any());
    }
}
