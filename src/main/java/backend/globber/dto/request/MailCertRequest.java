package backend.globber.dto.request;

public record MailCertRequest(
    String email,
    String uuid
) {
}
