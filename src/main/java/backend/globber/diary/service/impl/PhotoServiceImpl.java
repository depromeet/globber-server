package backend.globber.diary.service.impl;

import static java.util.stream.Collectors.toList;

import backend.globber.common.service.UploadedImageValidator;
import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.diary.service.PhotoService;
import backend.globber.exception.spec.DuplicatePhotoCodeException;
import backend.globber.exception.spec.PhotoCountException;
import backend.globber.exception.spec.PhotoNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private static final int MAX_PHOTOS = 3;
    private final PhotoRepository photoRepository;
    private final UploadedImageValidator uploadedImageValidator;

    @Transactional
    @Override
    public void syncPhotos(Diary diary, List<PhotoRequest> requests) {
        validateRequests(requests);

        Map<Long, Photo> existingPhotos = diary.getPhotos().stream()
            .collect(Collectors.toMap(Photo::getId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (PhotoRequest request : requests) {
            if (request.photoId() == null) {
                continue;
            }
            if (!existingPhotos.containsKey(request.photoId())) {
                throw new PhotoNotFoundException(
                    "해당 기록에 속한 사진이 아닙니다. (photoId=" + request.photoId() + ")");
            }
            keepIds.add(request.photoId());
        }

        // 변경을 시작하기 전에 새 사진을 모두 검사한다 (실패하면 아무것도 바뀌지 않는다)
        validateNewPhotos(requests, existingPhotos, keepIds);

        // 목록에 없는 기존 사진 삭제 (photoCode unique 충돌을 피하려고 새 사진 insert 전에 반영)
        if (diary.getPhotos().removeIf(photo -> !keepIds.contains(photo.getId()))) {
            photoRepository.flush();
        }

        for (int order = 0; order < requests.size(); order++) {
            PhotoRequest request = requests.get(order);
            if (request.photoId() == null) {
                createPhoto(diary, request, order);
                continue;
            }
            Photo photo = existingPhotos.get(request.photoId());
            photo.updateMetadata(
                request.lat(),
                request.lng(),
                request.width(),
                request.height(),
                request.takenMonth(),
                request.tag(),
                request.placeName()
            );
            photo.changeDisplayOrder(order);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<PhotoResponse> getAllPhoto(Long diaryId) {
        return photoRepository.findAllByDiaryIdOrderByDisplayOrderAscIdAsc(diaryId)
            .stream()
            .map(this::toResponse)
            .collect(toList());
    }

    @Transactional(readOnly = true)
    @Override
    public PhotoResponse getPhotoId(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new PhotoNotFoundException("사진을 찾을 수 없습니다."));
        return toResponse(photo);
    }

    private void validateRequests(List<PhotoRequest> requests) {
        if (requests == null) {
            throw new PhotoCountException("사진 목록은 필수입니다.");
        }
        if (requests.size() > MAX_PHOTOS) {
            throw new PhotoCountException("사진은 최대 " + MAX_PHOTOS + "장까지만 업로드할 수 있습니다.");
        }
        Set<Long> photoIds = new HashSet<>();
        for (PhotoRequest request : requests) {
            if (request.photoId() != null && !photoIds.add(request.photoId())) {
                throw new IllegalArgumentException(
                    "같은 사진이 중복으로 요청되었습니다. (photoId=" + request.photoId() + ")");
            }
        }
    }

    /**
     * 새로 추가되는 사진(photoId 없음)만 검사한다. 기존 사진은 요청의 photoCode를 쓰지 않으므로 검사하지 않는다.
     * - 새 사진끼리, 또는 유지하는 기존 사진과 photoCode가 같으면 400
     * - 다른 기록이 이미 쓰는 photoCode면 409 (이 기록에서 지워지는 사진의 코드는 다시 써도 된다)
     * - 업로드된 이미지의 형식·크기 검사(S3 HEAD)
     */
    private void validateNewPhotos(List<PhotoRequest> requests, Map<Long, Photo> existingPhotos, Set<Long> keepIds) {
        Set<String> keptCodes = new HashSet<>();
        Set<String> ownCodes = new HashSet<>();
        existingPhotos.forEach((id, photo) -> {
            ownCodes.add(photo.getPhotoCode());
            if (keepIds.contains(id)) {
                keptCodes.add(photo.getPhotoCode());
            }
        });

        Set<String> newCodes = new HashSet<>();
        for (PhotoRequest request : requests) {
            if (request.photoId() != null) {
                continue;
            }
            String code = request.photoCode();
            if (!newCodes.add(code) || keptCodes.contains(code)) {
                throw new IllegalArgumentException("같은 사진이 중복으로 요청되었습니다. (photoCode=" + code + ")");
            }
            if (!ownCodes.contains(code) && photoRepository.existsByPhotoCode(code)) {
                throw new DuplicatePhotoCodeException();
            }
        }
        newCodes.forEach(uploadedImageValidator::validate);
    }

    private void createPhoto(Diary diary, PhotoRequest request, int displayOrder) {
        Photo photo = Photo.builder()
            .photoCode(request.photoCode())
            .lat(request.lat())
            .lng(request.lng())
            .width(request.width())
            .height(request.height())
            .takenMonth(request.takenMonth())
            .tag(request.tag())
            .placeName(request.placeName())
            .displayOrder(displayOrder)
            .diary(diary)
            .build();

        diary.getPhotos().add(photo);
        photoRepository.save(photo);
    }

    private PhotoResponse toResponse(Photo photo) {
        return new PhotoResponse(
            photo.getId(),
            photo.getPhotoCode(),
            photo.getLat(),
            photo.getLng(),
            photo.getWidth(),
            photo.getHeight(),
            photo.getTakenMonth(),
            photo.getPlaceName(),
            photo.getTag()
        );
    }
}
