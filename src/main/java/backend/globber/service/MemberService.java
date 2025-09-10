package backend.globber.service;

import backend.globber.dto.response.MemberResponse;
import java.util.List;

public interface MemberService {
    void saveLocalMember(String name, String email, String password);

    boolean checkMemberEmail(String email);

    void sendCertMail(String email);

    boolean checkCertMail(String email, String uuid);

    void changePassword(String accesstoken, String oldPassword, String newPassword);

    void changeName(String accesstoken, String name);

    MemberResponse findMember(String accesstoken);


    // For test
    List<MemberResponse> findAllMember();

}