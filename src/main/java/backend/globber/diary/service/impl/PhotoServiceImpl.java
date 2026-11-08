package backend.globber.diary.service.impl;

import static java.util.stream.Collectors.toList;

import backend.globber.common.service.UploadedImageValidator;
import backend.globber.exception.spec.InvalidPhotoSizeException;
import backend.globber.exception.spec.InvalidS3KeyException;
import backend.globber.common.service.S3ObjectDeleter;
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
import java.util.Locale;
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
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "avif");
    private final PhotoRepository photoRepository;
    private final UploadedImageValidator uploadedImageValidator;
    private final S3ObjectDeleter s3ObjectDeleter;

    @Transactional
    @Override
    public void syncPhotos(Long memberId, Diary diary, List<PhotoRequest> requests) {
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
        Set<String> newCodes = validateNewPhotos(memberId, requests, existingPhotos, keepIds);

        // 목록에서 빠진 사진의 S3 객체는 커밋 뒤 지운다. 같은 키를 새 사진이 다시 쓰면 지우지 않는다
        List<String> removedCodes = diary.getPhotos().stream()
            .filter(photo -> !keepIds.contains(photo.getId()))
            .map(Photo::getPhotoCode)
            .filter(code -> !newCodes.contains(code))
            .toList();

        // 목록에 없는 기존 사진 삭제 (photoCode unique 충돌을 피하려고 새 사진 insert 전에 반영)
        if (diary.getPhotos().removeIf(photo -> !keepIds.contains(photo.getId()))) {
            photoRepository.flush();
        }
        if (!removedCodes.isEmpty()) {
            s3ObjectDeleter.deleteAfterCommit(removedCodes);
        }

        for (int order = 0; order < requests.size(); order++) {
            PhotoRequest request = requests.get(order);
            if (request.photoId() == null) {
                createPhoto(diary, request, order);
                continue;
            }
            Photo photo = existingPhotos.get(request.photoId());
            // 기존 사진은 1 미만 크기가 와도 거절하지 않고 저장된 크기를 그대로 둔다(예전 0 크기 데이터의 수정 저장을 막지 않기 위해)
            boolean validSize = isValidSize(request.width()) && isValidSize(request.height());
            photo.updateMetadata(
                request.lat(),
                request.lng(),
                validSize ? request.width() : photo.getWidth(),
                validSize ? request.height() : photo.getHeight(),
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
     * - width·height가 없거나 1 미만이면 400
     * - photoCode가 본인 경로(travels/{memberId}/)가 아니거나 확장자가 허용 목록에 없으면 400
     * - 새 사진끼리, 또는 유지하는 기존 사진과 photoCode가 같으면 400
     * - 다른 기록이 이미 쓰는 photoCode면 409 (이 기록에서 지워지는 사진의 코드는 다시 써도 된다)
     * - 업로드된 이미지의 형식·크기 검사(S3 HEAD)
     */
    private Set<String> validateNewPhotos(Long memberId, List<PhotoRequest> requests, Map<Long, Photo> existingPhotos,
                                          Set<Long> keepIds) {
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
            if (!isValidSize(request.width()) || !isValidSize(request.height())) {
                throw new InvalidPhotoSizeException();
            }
            String code = request.photoCode();
            validateOwnedPath(memberId, code);
            if (!newCodes.add(code) || keptCodes.contains(code)) {
                throw new IllegalArgumentException("같은 사진이 중복으로 요청되었습니다. (photoCode=" + code + ")");
            }
            if (!ownCodes.contains(code) && photoRepository.existsByPhotoCode(code)) {
                throw new DuplicatePhotoCodeException();
            }
        }
        newCodes.forEach(uploadedImageValidator::validate);
        return newCodes;
    }

    private static boolean isValidSize(Long size) {
        return size != null && size >= 1;
    }

    private void validateOwnedPath(Long memberId, String photoCode) {
        String ownPrefix = "travels/" + memberId + "/";
        int dot = photoCode == null ? -1 : photoCode.lastIndexOf('.');
        boolean owned = photoCode != null && photoCode.startsWith(ownPrefix) && photoCode.length() > ownPrefix.length();
        boolean allowedExtension = dot > ownPrefix.length() - 1
            && ALLOWED_EXTENSIONS.contains(photoCode.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (!owned || !allowedExtension) {
            throw new InvalidS3KeyException("유효하지 않은 사진 경로입니다.");
        }
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
