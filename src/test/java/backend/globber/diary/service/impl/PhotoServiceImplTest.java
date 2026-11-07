package backend.globber.diary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.exception.spec.PhotoCountException;
import backend.globber.exception.spec.PhotoNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private PhotoServiceImpl photoService;

    private Diary diary;

    @BeforeEach
    void setUp() {
        diary = Diary.builder().text("본문").build();
        for (long id = 1; id <= 3; id++) {
            diary.getPhotos().add(Photo.builder()
                .id(id)
                .photoCode("code-" + id)
                .displayOrder((int) id - 1)
                .diary(diary)
                .build());
        }
    }

    @Test
    @DisplayName("사진 목록이 null이면 거절한다")
    void syncPhotos_nullRequests() {
        // when & then
        assertThatThrownBy(() -> photoService.syncPhotos(diary, null))
            .isInstanceOf(PhotoCountException.class);
        assertThat(diary.getPhotos()).hasSize(3);
    }

    @Test
    @DisplayName("사진이 3장을 넘으면 거절한다")
    void syncPhotos_tooManyPhotos() {
        // given
        List<PhotoRequest> requests = List.of(
            request(1L, "code-1"), request(2L, "code-2"), request(3L, "code-3"), request(null, "new"));

        // when & then
        assertThatThrownBy(() -> photoService.syncPhotos(diary, requests))
            .isInstanceOf(PhotoCountException.class)
            .hasMessage("사진은 최대 3장까지만 업로드할 수 있습니다.");
        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 photoId가 중복되면 거절하고 아무것도 바꾸지 않는다")
    void syncPhotos_duplicatePhotoId() {
        // given
        List<PhotoRequest> requests = List.of(request(1L, "code-1"), request(1L, "code-1"));

        // when & then
        assertThatThrownBy(() -> photoService.syncPhotos(diary, requests))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(diary.getPhotos()).extracting(Photo::getId).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("기록에 없는 photoId가 오면 PhotoNotFoundException을 던지고 아무것도 바꾸지 않는다")
    void syncPhotos_foreignPhotoId() {
        // given
        List<PhotoRequest> requests = List.of(request(99L, "other"), request(null, "new"));

        // when & then
        assertThatThrownBy(() -> photoService.syncPhotos(diary, requests))
            .isInstanceOf(PhotoNotFoundException.class);
        assertThat(diary.getPhotos()).extracting(Photo::getId).containsExactly(1L, 2L, 3L);
        verify(photoRepository, never()).flush();
        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("배열 인덱스를 표시 순서로 저장하고, 빠진 사진은 삭제를 먼저 반영한 뒤 새 사진을 추가한다")
    void syncPhotos_mixed() {
        // given: 1,2,3 중 2 삭제, 3 수정, 새 사진 추가, 순서 [3, 새 사진, 1]
        List<PhotoRequest> requests = new ArrayList<>(List.of(
            new PhotoRequest(3L, "code-3", null, null, 10L, 20L, null, null, "바뀐 장소"),
            request(null, "new"),
            request(1L, "code-1")));

        // when
        photoService.syncPhotos(diary, requests);

        // then
        assertThat(diary.getPhotos().stream().sorted(Photo.DISPLAY_ORDER).map(Photo::getPhotoCode))
            .containsExactly("code-3", "new", "code-1");
        assertThat(diary.getPhotos()).extracting(Photo::getDisplayOrder).containsExactlyInAnyOrder(0, 1, 2);
        Photo updated = diary.getPhotos().stream().filter(p -> "code-3".equals(p.getPhotoCode())).findFirst().get();
        assertThat(updated.getPlaceName()).isEqualTo("바뀐 장소");
        assertThat(updated.getWidth()).isEqualTo(10L);

        InOrder inOrder = inOrder(photoRepository);
        inOrder.verify(photoRepository).flush();
        inOrder.verify(photoRepository).save(any(Photo.class));
    }

    private PhotoRequest request(Long photoId, String photoCode) {
        return new PhotoRequest(photoId, photoCode, null, null, 100L, 100L, null, null, null);
    }
}
