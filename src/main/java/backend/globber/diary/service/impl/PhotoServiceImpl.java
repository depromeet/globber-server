package backend.globber.diary.service.impl;

import static java.util.stream.Collectors.toList;

import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.diary.service.PhotoService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.PhotoCountException;
import backend.globber.exception.spec.PhotoNotFoundException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private static final int MAX_PHOTOS = 3;
    private final PhotoRepository photoRepository;
    private final DiaryRepository diaryRepository;

    @Transactional
    @Override
    public PhotoResponse savePhoto(Long memberId, Long diaryId, PhotoRequest request) {
        // 일기장의 소유자 확인
        if(!Objects.equals(memberId, diaryRepository.findMemberIdById(diaryId))) {
            throw new DiaryNotFoundException("해당 기록의 소유자가 아닙니다.");
        }

        // 일기장 존재 여부 확인
        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        // 3장 제한 검증
        int currentCount = diary.getPhotos().size();
        if (currentCount >= MAX_PHOTOS) {
            throw new PhotoCountException("사진은 최대 " + MAX_PHOTOS + "장까지만 업로드할 수 있습니다.");
        }

        Photo photo = Photo.builder()
            .photoCode(request.photoCode())
            .lat(request.lat())
            .lng(request.lng())
            .width(request.width())
            .height(request.height())
            .takenMonth(request.takenMonth())
            .tag(request.tag())
            .placeName(request.placeName())
            .diary(diary)
            .build();

        diary.getPhotos().add(photo);
        photoRepository.save(photo);
        return toResponse(photo);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PhotoResponse> getAllPhoto(Long diaryId) {
        return photoRepository.findAllByDiaryId(diaryId)
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

    @Transactional
    @Override
    public PhotoResponse updatePhoto(Long memberId, Long diaryId, PhotoRequest request) {
        // 일기장의 소유자 확인
        if(!Objects.equals(memberId, diaryRepository.findMemberIdById(diaryId))) {
            throw new DiaryNotFoundException("해당 기록의 소유자가 아닙니다.");
        }
        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));
        Photo photo = photoRepository.findById(request.photoId())
            .orElseThrow(() -> new PhotoNotFoundException("사진을 찾을 수 없습니다."));

        if(!diary.getPhotos().contains(photo)) {
            throw new PhotoNotFoundException("해당 기록에 속한 사진이 아닙니다.");
        }

        photo.updateMetadata(
            request.lat(),
            request.lng(),
            request.width(),
            request.height(),
            request.takenMonth(),
            request.tag(),
            request.placeName()
        );

        return toResponse(photo);
    }

    @Transactional
    @Override
    public void deletePhoto(Long memberId, Long diaryId, Long photoId) {
        // 일기장의 소유자 확인
        if(!Objects.equals(memberId, diaryRepository.findMemberIdById(diaryId))) {
            throw new DiaryNotFoundException("해당 기록의 소유자가 아닙니다.");
        }
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new PhotoNotFoundException("사진을 찾을 수 없습니다."));
        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if(diary.getPhotos().contains(photo)) {
            diary.getPhotos().remove(photo);
        }
        else {
            throw new PhotoNotFoundException("해당 기록에 속한 사진이 아닙니다.");
        }

        photoRepository.delete(photo);
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
