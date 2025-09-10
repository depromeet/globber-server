package backend.globber.dto.request;

public record MemberRequest(
    String email,
    String password,
    String name
) {

}
