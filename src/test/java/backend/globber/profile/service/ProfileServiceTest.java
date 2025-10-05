package backend.globber.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.profile.controller.dto.request.UpdateProfileImageRequest;
import backend.globber.profile.controller.dto.request.UpdateProfileRequest;
import backend.globber.profile.controller.dto.response.ProfileResponse;
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
class ProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ProfileService profileService;

    private Member member;
    private final String s3BaseUrl = "https://test-bucket.s3.amazonaws.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileService, "s3BaseUrl", s3BaseUrl);

        member = Member.of(
            "test@kakao",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER)
        );
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    @DisplayName("회원의 프로필을 조회한다")
    void getProfile() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        ProfileResponse response = profileService.getProfile(memberId);

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("테스트유저");
        assertThat(response.email()).isEqualTo("test@kakao");
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.authProvider()).isEqualTo(AuthProvider.KAKAO);
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("프로필 이미지가 있는 경우 full URL을 반환한다")
    void getProfileWithImage() {
        // given
        Long memberId = 1L;
        String s3Key = "profiles/1/test-image.jpg";
        member.changeProfileImage(s3Key);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        ProfileResponse response = profileService.getProfile(memberId);

        // then
        assertThat(response.profileImageUrl()).isEqualTo(s3BaseUrl + "/" + s3Key);
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외가 발생한다")
    void getProfileNotFound() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileService.getProfile(memberId))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("존재하지 않는 멤버입니다.");
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("회원의 닉네임을 수정한다")
    void updateNickname() {
        // given
        Long memberId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        ProfileResponse response = profileService.updateProfile(memberId, request);

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(member.getName()).isEqualTo("새닉네임");
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 닉네임 수정 시 예외가 발생한다")
    void updateNicknameNotFound() {
        // given
        Long memberId = 999L;
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임");
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileService.updateProfile(memberId, request))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("존재하지 않는 멤버입니다.");
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("회원의 프로필 이미지를 수정한다")
    void updateImage() {
        // given
        Long memberId = 1L;
        String s3Key = "profiles/1/new-image.jpg";
        UpdateProfileImageRequest request = new UpdateProfileImageRequest(s3Key);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        ProfileResponse response = profileService.updateProfileImage(memberId, request);

        // then
        assertThat(response.profileImageUrl()).isEqualTo(s3BaseUrl + "/" + s3Key);
        assertThat(member.getProfileImageKey()).isEqualTo(s3Key);
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("기존 프로필 이미지를 새 이미지로 덮어쓴다")
    void updateImageOverwrite() {
        // given
        Long memberId = 1L;
        String oldS3Key = "profiles/1/old-image.jpg";
        String newS3Key = "profiles/1/new-image.jpg";
        member.changeProfileImage(oldS3Key);

        UpdateProfileImageRequest request = new UpdateProfileImageRequest(newS3Key);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        ProfileResponse response = profileService.updateProfileImage(memberId, request);

        // then
        assertThat(response.profileImageUrl()).isEqualTo(s3BaseUrl + "/" + newS3Key);
        assertThat(member.getProfileImageKey()).isEqualTo(newS3Key);
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필 이미지 수정 시 예외가 발생한다")
    void updateImageNotFound() {
        // given
        Long memberId = 999L;
        UpdateProfileImageRequest request = new UpdateProfileImageRequest("profiles/999/image.jpg");
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileService.updateProfileImage(memberId, request))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("존재하지 않는 멤버입니다.");
        verify(memberRepository, times(1)).findById(memberId);
    }
}
