package backend.globber.service;

import backend.globber.domain.Member;
import backend.globber.domain.constant.Role;
import backend.globber.exception.spec.UsernameNotFoundException;
import backend.globber.repository.MemberRepository;
import java.util.ArrayList;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;



@RequiredArgsConstructor
@Service
@Slf4j
public class SecurityUserDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
            .orElseThrow(UsernameNotFoundException::new);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : member.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.name()));
        }
        return org.springframework.security.core.userdetails.User.builder()
            .username(member.getEmail())
            .password(member.getPassword())
            .authorities(authorities)
            .build();
    }
}