package backend.globber.profile.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.exception.spec.InvalidS3KeyException;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.profile.controller.dto.request.UpdateProfileImageRequest;
import backend.globber.profile.controller.dto.request.UpdateProfileRequest;
import backend.globber.profile.controller.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final MemberRepository memberRepository;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    public ProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));
        return ProfileResponse.from(member, s3BaseUrl);
    }

    @Transactional
    public ProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        member.changeName(request.nickname());

        return ProfileResponse.from(member, s3BaseUrl);
    }

    @Transactional
    public ProfileResponse updateProfileImage(Long memberId, UpdateProfileImageRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 멤버입니다."));

        String expectedPrefix = "profiles/" + memberId + "/";
        if (!request.s3Key().startsWith(expectedPrefix)) {
            log.warn("잘못된 s3Key 접근 시도 - memberId: {}, s3Key: {}", memberId, request.s3Key());
            throw new InvalidS3KeyException("유효하지 않은 이미지 경로입니다.");
        }

        member.changeProfileImage(request.s3Key());

        return ProfileResponse.from(member, s3BaseUrl);
    }
}
