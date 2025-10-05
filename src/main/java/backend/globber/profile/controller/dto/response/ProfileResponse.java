package backend.globber.profile.controller.dto.response;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import lombok.Builder;

@Builder
public record ProfileResponse(
    long memberId,
    String nickname,
    String email,
    String profileImageUrl,
    AuthProvider authProvider
) {

    public static ProfileResponse from(Member member, String s3BaseUrl) {
        String imageKey = member.getProfileImageKey();
        String fullUrl = (imageKey != null && !imageKey.isEmpty())
            ? s3BaseUrl + "/" + imageKey
            : null;

        return ProfileResponse.builder()
            .memberId(member.getId())
            .nickname(member.getName())
            .email(member.getEmail())
            .profileImageUrl(fullUrl)
            .authProvider(member.getAuthProvider())
            .build();
    }
}
