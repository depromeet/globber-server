package backend.globber.service.impl;

import backend.globber.domain.Member;
import backend.globber.domain.constant.AuthProvider;
import backend.globber.domain.constant.Role;
import backend.globber.dto.response.MemberResponse;
import backend.globber.exception.spec.NoCredException;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.repository.MemberRepository;
import backend.globber.service.MemberService;
import backend.globber.util.JwtTokenProvider;
import backend.globber.util.MailProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MailProvider mailProvider;
    private final JwtTokenProvider tokenUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void saveLocalMember(String name, String email, String password) {
        if (mailProvider.checkAck(email)) {
            List<Role> roles = List.of(Role.ROLE_USER);
            Member member = Member.of(email, name, password, AuthProvider.LOCAL, roles);
            memberRepository.save(member);
        }
        else {
            throw new NoCredException();
        }
    }

    @Override
    public boolean checkMemberEmail(String email) {
        return !memberRepository.existsByEmail(email);
    }

    @Override
    public void sendCertMail(String email) {
        mailProvider.sendMail(email);
    }

    @Override
    public boolean checkCertMail(String email, String uuid) {
        return mailProvider.checkMail(email, uuid);
    }

    @Override
    public void changePassword(String accesstoken, String password, String newPassword) {
        Member member = memberRepository.findByEmail(tokenUtil.getEmailForAccessToken(accesstoken))
            .orElseThrow(UsernameNotFoundException::new);
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new UsernameNotFoundException("비밀번호가 일치하지 않습니다.");
        }
        member.changePassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Override
    public void changeName(String accesstoken, String name) {
        Member member = memberRepository.findByEmail(tokenUtil.getEmailForAccessToken(accesstoken))
            .orElseThrow(UsernameNotFoundException::new);
        member.changeName(name);
        memberRepository.save(member);
    }

    @Override
    public MemberResponse findMember(String accessToken) {
        return memberRepository.findByEmail(tokenUtil.getEmailForAccessToken(accessToken))
            .map(MemberResponse::toResponse)
            .orElseThrow(UsernameNotFoundException::new);
    }

    @Override
    public List<MemberResponse> findAllMember() {
        return memberRepository.findAll()
            .stream()
            .map(MemberResponse::toResponse)
            .toList();
    }
}
