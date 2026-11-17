package backend.globber.auth.domain;

import backend.globber.common.util.S3Urls;

import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.domain.converter.AuthProviderConverter;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
@Table(
        indexes = {
                @Index(columnList = "email", unique = true),
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE member SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Member {

    private static final java.util.regex.Pattern SYNTHETIC_KAKAO_EMAIL = java.util.regex.Pattern.compile("\\d+@kakao");

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private final List<Role> roles = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String email;
    @Column(nullable = false, length = 30)
    private String name;
    private String password;
    @Convert(converter = AuthProviderConverter.class)
    @Column(nullable = false)
    private AuthProvider authProvider;
    @Column(nullable = false, unique = true, updatable = false, length = 6)
    private String uuid;  // 공유 링크 식별자
    private boolean isFirstLogin;
    @Column(length = 500)
    private String profileImageKey;

    // 소프트 딜리트
    @Column(nullable = false)
    private boolean deleted = false;

    // 탈퇴 시각
    private LocalDateTime deletedAt;

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
            List<Role> roles,
            String uuid
    ) {
        return new Member(email, name, password, authProvider, roles, uuid);
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

    public void changeUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * 연락용 이메일. 카카오 로그인은 '{카카오 회원번호}@kakao'를 로그인 식별자(email 컬럼)로 저장하는데,
     * 이 값은 실제 이메일이 아니므로 응답에는 null로 내보낸다(GLB-325). 로그인 식별자 자체는 바꾸지 않는다.
     */
    public String getContactEmail() {
        if (email == null || SYNTHETIC_KAKAO_EMAIL.matcher(email).matches()) {
            return null;
        }
        return email;
    }

    public String getProfileImageUrl(String s3BaseUrl) {
        return S3Urls.join(s3BaseUrl, profileImageKey);
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
