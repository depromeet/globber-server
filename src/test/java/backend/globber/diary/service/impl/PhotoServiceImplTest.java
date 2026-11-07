package backend.globber.diary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.globber.common.service.UploadedImageValidator;
import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.exception.spec.InvalidUploadFileException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DIARY_ID = 10L;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private UploadedImageValidator uploadedImageValidator;

    @InjectMocks
    private PhotoServiceImpl photoService;

    private Diary diary;

    @BeforeEach
    void setUp() {
        diary = Diary.builder().text("기록").build();
        ReflectionTestUtils.setField(diary, "id", DIARY_ID);
    }

    private PhotoRequest photoRequest(Long photoId, String photoCode) {
        return new PhotoRequest(photoId, photoCode, 37.5, 127.0, 100L, 100L, YearMonth.of(2025, 10), null, null);
    }

    @Test
    @DisplayName("사진을 추가할 때 업로드된 이미지를 검증한 뒤 저장한다")
    void savePhotoValidatesUploadedImage() {
        // given
        String photoCode = "travels/1/image.jpg";
        given(diaryRepository.findMemberIdById(DIARY_ID)).willReturn(MEMBER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));

        // when
        PhotoResponse response = photoService.savePhoto(MEMBER_ID, DIARY_ID, photoRequest(null, photoCode));

        // then
        verify(uploadedImageValidator).validate(photoCode);
        verify(photoRepository).save(any(Photo.class));
        assertThat(response.photoCode()).isEqualTo(photoCode);
        assertThat(diary.getPhotos()).hasSize(1);
    }

    @Test
    @DisplayName("업로드된 이미지 검증에 실패하면 사진을 저장하지 않는다")
    void savePhotoValidationFail() {
        // given
        String photoCode = "travels/1/image.heic";
        given(diaryRepository.findMemberIdById(DIARY_ID)).willReturn(MEMBER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        willThrow(new InvalidUploadFileException("지원하지 않는 이미지 형식입니다."))
            .given(uploadedImageValidator).validate(photoCode);

        // when & then
        assertThatThrownBy(() -> photoService.savePhoto(MEMBER_ID, DIARY_ID, photoRequest(null, photoCode)))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("지원하지 않는 이미지 형식입니다.");
        verify(photoRepository, never()).save(any(Photo.class));
        assertThat(diary.getPhotos()).isEmpty();
    }

    @Test
    @DisplayName("기존 사진의 메타데이터를 수정할 때는 업로드된 이미지를 검증하지 않는다")
    void updatePhotoDoesNotValidate() {
        // given
        Photo photo = Photo.builder().photoCode("travels/1/old.heic").diary(diary).build();
        ReflectionTestUtils.setField(photo, "id", 100L);
        diary.getPhotos().add(photo);
        given(diaryRepository.findMemberIdById(DIARY_ID)).willReturn(MEMBER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        given(photoRepository.findById(100L)).willReturn(Optional.of(photo));

        // when
        photoService.updatePhoto(MEMBER_ID, DIARY_ID, List.of(photoRequest(100L, "travels/1/old.heic")));

        // then
        verifyNoInteractions(uploadedImageValidator);
        assertThat(photo.getLat()).isEqualTo(37.5);
    }
}
