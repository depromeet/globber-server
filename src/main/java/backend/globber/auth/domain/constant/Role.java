package backend.globber.auth.domain.constant;

import lombok.Getter;


@Getter
public enum Role {

    ROLE_USER("ROLE_USER"),
    ROLE_UNAUTHORIZED("ROLE_UNAUTHORIZED"),
    ROLE_ADMIN("ROLE_ADMIN");

    final String role;

    Role(String role) {
        this.role = role;
    }
}
