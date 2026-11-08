package backend.globber.diary.service.impl;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.controller.dto.CityResponse;
import backend.globber.city.domain.City;
import backend.globber.city.repository.CityRepository;
import backend.globber.diary.controller.dto.CityGroupedDiaryResponse;
import backend.globber.diary.controller.dto.DiaryListResponse;
import backend.globber.diary.controller.dto.DiaryRequest;
import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.controller.dto.PhotoResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.diary.repository.DiaryEmojiReactionRepository;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.diary.repository.DiaryRepository;
import backend.globber.diary.repository.PhotoRepository;
import backend.globber.diary.service.DiaryService;
import backend.globber.diary.service.PhotoService;
import backend.globber.exception.spec.CityNotFoundException;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.NoCredException;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.membertravel.repository.MemberTravelCityRepository;
import backend.globber.membertravel.repository.MemberTravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final CityRepository cityRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PhotoService photoService;
    private final MemberTravelCityRepository memberTravelCityRepository;
    private final MemberTravelRepository memberTravelRepository;

    private final DiaryEmojiRepository emojiRepository;
    private final DiaryEmojiReactionRepository emojiReactionRepository;

    @Transactional
    @Override
    public DiaryResponse createDiaryWithPhoto(Long memberId, DiaryRequest diaryRequest) {

        // 해당사용자가 해당 여행지를 선택했는지 검증.
        Member member = memberRepository.findById(memberId)
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

        // Diary 생성
        Diary diary = Diary.builder()
                .memberTravelCity(MemberTraveledCity)
                .text(diaryRequest.text())
                .build();
        diaryRepository.save(diary);

        // Photo 저장 (배열 순서 = 표시 순서)
        photoService.syncPhotos(diary, diaryRequest.photos());

        // 저장된 Diary + Photo 리스트 변환
        return toDiaryResponse(diaryRepository.findById(diary.getId()).get(), Collections.emptyList());
    }

    @Transactional
    @Override
    public DiaryResponse updateDiary(Long memberId, Long diaryId, DiaryRequest diaryRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findWithCityById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if (!diary.getMemberTravelCity().getMemberTravel().getMember().getId().equals(member.getId())) {
            throw new NoCredException("본인의 기록만 수정할 수 있습니다.");
        }

        // 본문과 최종 사진 목록을 같은 트랜잭션에서 반영
        diary.changeText(diaryRequest.text());
        photoService.syncPhotos(diary, diaryRequest.photos());
        List<EmojiResponse> emojis = getEmojiResponses(diaryId, memberId);
        return toDiaryResponse(diary, emojis);
    }

    @Transactional
    @Override
    public void deleteDiary(Long memberId, Long diaryId) {
        Member member = memberRepository.findById(memberId)
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
    public DiaryResponse getDiaryDetail(Long memberId, Long diaryId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoCredException("회원 정보를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        if (!diary.getMemberTravelCity().getMemberTravel().getMember().getId().equals(member.getId())) {
            throw new NoCredException("본인의 기록만 조회할 수 있습니다.");
        }

        List<EmojiResponse> emojis = getEmojiResponses(diaryId, memberId);

        return toDiaryResponse(diary, emojis);
    }

    @Transactional(readOnly = true)
    @Override
    public DiaryResponse getPublicDiaryDetail(Long diaryId, Long viewerId) {
        Diary diary = diaryRepository.findWithCityById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException("기록을 찾을 수 없습니다."));

        List<EmojiResponse> emojis = getEmojiResponses(diaryId, viewerId);

        return toDiaryResponse(diary, emojis);
    }

    @Transactional(readOnly = true)
    @Override
    public DiaryListResponse getDiariesByUUID(String uuid, Long viewerId) {
        List<Diary> diaries = diaryRepository.findAllWithRelationsByMemberUuid(uuid);

        // 조회한 회원이 누른 이모지를 기록별로 한 번에 읽어 둔다 (기록마다 다시 조회하지 않도록)
        Map<Long, Set<String>> pressedByDiary = pressedCodes(viewerId, diaries);

        // Diary → DiaryResponse 변환
        List<DiaryResponse> diaryResponses = diaries.stream()
                .map(diary -> {
                    List<EmojiResponse> emojis = getEmojiResponses(diary.getId(),
                            pressedByDiary.getOrDefault(diary.getId(), Set.of()));
                    return toDiaryResponse(diary, emojis);
                })
                .toList();

        // cityId 기준으로 그룹핑
        Map<Long, List<DiaryResponse>> grouped = diaryResponses.stream()
                .collect(Collectors.groupingBy(d -> d.city().cityId()));

        // cityId 별 CityGroupedDiaryResponse 생성
        List<CityGroupedDiaryResponse> cityGroups = grouped.entrySet().stream()
                .map(entry -> new CityGroupedDiaryResponse(
                        entry.getValue().get(0).city(),  // 같은 cityId면 동일한 CityResponse
                        entry.getValue()
                ))
                .toList();

        return new DiaryListResponse(cityGroups);
    }


    private List<EmojiResponse> getEmojiResponses(Long diaryId, Long viewerId) {
        Set<String> pressed = viewerId == null
                ? Set.of()
                : new HashSet<>(emojiReactionRepository.findCodes(diaryId, viewerId));
        return getEmojiResponses(diaryId, pressed);
    }

    private List<EmojiResponse> getEmojiResponses(Long diaryId, Set<String> pressedCodes) {
        return emojiRepository
                .findAllByDiaryIdOrderByCountDescCreatedAtAsc(diaryId)
                .stream()
                .map(emoji -> EmojiResponse.from(emoji, pressedCodes.contains(emoji.getCode())))
                .toList();
    }

    private Map<Long, Set<String>> pressedCodes(Long viewerId, List<Diary> diaries) {
        if (viewerId == null || diaries.isEmpty()) {
            return Map.of();
        }
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        return emojiReactionRepository.findPressedCodes(viewerId, diaryIds).stream()
                .collect(Collectors.groupingBy(
                        DiaryEmojiReactionRepository.PressedCode::getDiaryId,
                        Collectors.mapping(DiaryEmojiReactionRepository.PressedCode::getCode, Collectors.toSet())));
    }

    private DiaryResponse toDiaryResponse(Diary diary, List<EmojiResponse> emojis) {
        List<PhotoResponse> photos = diary.getPhotos().stream()
                .sorted(Photo.DISPLAY_ORDER)
                .map(photo -> new PhotoResponse(
                        photo.getId(),
                        photo.getPhotoCode(),
                        photo.getLat(),
                        photo.getLng(),
                        photo.getWidth(),
                        photo.getHeight(),
                        photo.getTakenMonth(),
                        photo.getPlaceName(),
                        photo.getTag()
                ))
                .collect(toList());

        return new DiaryResponse(
                diary.getId(),
                CityResponse.toResponse(diary.getMemberTravelCity().getCity()),
                diary.getText(),
                diary.getCreatedAt().toString(),
                diary.getUpdatedAt().toString(),
                photos,
                emojis,
                diary.getMemberTravelCity().getMemberTravel().getMember().getId(),
                diary.getMemberTravelCity().getMemberTravel().getMember().getName(),
                diary.getMemberTravelCity().getMemberTravel().getMember().getProfileImageKey()
        );
    }
}