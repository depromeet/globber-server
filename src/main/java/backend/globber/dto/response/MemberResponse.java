package backend.globber.dto.response;

import backend.globber.domain.Member;

public record MemberResponse(
    String email,
    String name
) {

    public static MemberResponse toResponse(Member entity) {
        return new MemberResponse(
            entity.getEmail(),
            entity.getName()
        );
    }
}
