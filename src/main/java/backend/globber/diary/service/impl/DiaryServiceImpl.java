package backend.globber.diary.service.impl;

import static java.util.stream.Collectors.toList;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.controller.dto.PhotoRequest;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.diary.service.DiaryService;
import backend.globber.diary.service.PhotoService;
import backend.globber.exception.spec.CityNotFoundException;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.NoCredException;
import backend.globber.exception.spec.PhotoCountException;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private static final int MAX_PHOTOS = 3; // 한 게시물당 최대 3장
    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final CityRepository cityRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PhotoService photoService;
    private final MemberTravelCityRepository memberTravelCityRepository;
    private final MemberTravelRepository memberTravelRepository;

    @Transactional
    @Override
    public DiaryResponse createDiaryWithPhoto(String accessToken, DiaryRequest diaryRequest) {

        // 해당사용자가 해당 여행지를 선택했는지 검증.
        String email = jwtTokenProvider.getEmailForAccessToken(accessToken);
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));

        City city = cityRepository.findById(diaryRequest.cityId())
            .orElseThrow(() -> new CityNotFoundException("도시 정보를 찾을 수 없습니다."));

        // 회원의 여행 정보 조회
        MemberTravel memberTravel = memberTravelRepository.findByMember_Id(member.getId())
            .orElseThrow(() -> new NoCredException("회원의 여행 정보를 찾을 수 없습니다."));

        // 해당 도시를 여행지로 등록했는지 확인
        MemberTravelCity MemberTraveledCity = memberTravelCityRepository
            .findByMemberTravel_IdAndCity_CityId(memberTravel.getId(), city.getCityId())
            .orElseThrow(() -> new CityNotFoundException("해당 여행지를 선택한 사용자만 기록을 작성할 수 있습니다."));

        // 사진 개수 검증
        if (diaryRequest.photos().size() > MAX_PHOTOS) {
            throw new PhotoCountException("사진은 최대 " + MAX_PHOTOS + "장까지만 업로드할 수 있습니다.");
        }

        // Diary 생성
        Diary diary = Diary.builder()
            .memberTravelCity(MemberTraveledCity)
            .comment(diaryRequest.comment())
            .emoji(diaryRequest.emoji())
            .build();
        diaryRepository.save(diary);

        // Photo 저장
        for (PhotoRequest photoRequest : diaryRequest.photos()) {
            photoService.savePhoto(diary.getId(), photoRequest);
        }

        // 저장된 Diary + Photo 리스트 변환
        return toDiaryResponse(diaryRepository.findById(diary.getId()).get());
    }

    @Transactional
    @Override
    public DiaryResponse updateDiary(String accessToken, Long diaryId, DiaryRequest diaryRequest) {
        String email = jwtTokenProvider.getEmailForAccessToken(accessToken);
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if (!diary.getMemberTravelCity().getMemberTravel().getMember().getId().equals(member.getId())) {
            throw new NoCredException("본인의 기록만 수정할 수 있습니다.");
        }

        diary.update(diaryRequest.comment(), diaryRequest.emoji());
        return toDiaryResponse(diary);
    }

    @Transactional
    @Override
    public void deleteDiary(String accessToken, Long diaryId) {
        String email = jwtTokenProvider.getEmailForAccessToken(accessToken);
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if (!diary.getMemberTravelCity().getMemberTravel().getMember().getId().equals(member.getId())) {
            throw new NoCredException("본인의 기록만 삭제할 수 있습니다.");
        }

        diaryRepository.delete(diary);
    }

    @Transactional(readOnly = true)
    @Override
    public DiaryResponse getDiaryDetail(String accessToken, Long diaryId) {
        String email = jwtTokenProvider.getEmailForAccessToken(accessToken);
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new NoCredException("회원 정보를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if (!diary.getMemberTravelCity().getMemberTravel().getMember().getId().equals(member.getId())) {
            throw new NoCredException("본인의 기록만 조회할 수 있습니다.");
        }

        return toDiaryResponse(diary);
    }

    // Diary -> DiaryResponse 변환
    private DiaryResponse toDiaryResponse(Diary diary) {
        List<PhotoResponse> photos = diary.getPhotos().stream()
            .map(photo -> new PhotoResponse(
                photo.getId(),
                photo.getPhotoCode(),
                photo.getLat(),
                photo.getLng(),
                photo.getWidth(),
                photo.getHeight(),
                photo.getTakenMonth(),
                photo.getTag()
            ))
            .collect(toList());

        return new DiaryResponse(
            diary.getId(),
            diary.getMemberTravelCity().getCity(),
            diary.getComment(),
            diary.getEmoji(),
            diary.getCreatedAt().toString(),
            diary.getUpdatedAt().toString(),
            photos
        );
    }
}
