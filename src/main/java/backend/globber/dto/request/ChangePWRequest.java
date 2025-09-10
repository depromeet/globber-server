package backend.globber.dto.request;

public record ChangePWRequest(
    String oldPassword,
    String newPassword

){

}
