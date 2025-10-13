package backend.globber.auth.domain;

import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.domain.converter.AuthProviderConverter;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(
        indexes = {
                @Index(columnList = "email", unique = true),
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private final List<Role> roles = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String email;
    @Column(nullable = false, length = 30)
    private String name;
    private String password;
    @Convert(converter = AuthProviderConverter.class)
    @Column(nullable = false)
    private AuthProvider authProvider;
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;  // 공유 링크 식별자
    private boolean isFirstLogin;
    @Column(length = 500)
    private String profileImageKey;


    // -- 생성자 메서드 -- //
    private Member(String email, String name, String password, AuthProvider authProvider,
                   List<Role> roles, String uuid) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.authProvider = authProvider;
        this.roles.addAll(roles);
        this.uuid = uuid;
        this.isFirstLogin = true;
    }

    public static Member of(
            String email,
            String name,
            String password,
            AuthProvider authProvider,
            List<Role> roles
    ) {
        return new Member(email, name, password, authProvider, roles, UUID.randomUUID().toString());
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeFirstLogin() {
        this.isFirstLogin = false;
    }

    public void changeProfileImage(String s3Key) {
        this.profileImageKey = s3Key;
    }

    public String getProfileImageUrl(String s3BaseUrl) {
        if (StringUtils.isEmpty(profileImageKey)) {
            return null;
        }
        return s3BaseUrl + "/" + profileImageKey;
    }

    // -- 비지니스 로직 (검증, setter) -- //

    // -- Equals & Hash -- //
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member member)) {
            return false;
        }
        return Objects.equals(id, member.id) && Objects.equals(email, member.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
