package backend.globber.auth.domain.converter;

import backend.globber.auth.domain.constant.AuthProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter
public class AuthProviderConverter implements AttributeConverter<AuthProvider, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AuthProvider authProvider) {
        if (authProvider == null) return null;
        return authProvider.getCode();
    }

    @Override
    public AuthProvider convertToEntityAttribute(Integer code) {
        if (code == null) return null;
        return AuthProvider.of(code);
    }
}
