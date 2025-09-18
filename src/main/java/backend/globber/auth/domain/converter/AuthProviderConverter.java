package backend.globber.auth.domain.converter;

import backend.globber.auth.domain.constant.AuthProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter
public class AuthProviderConverter implements AttributeConverter<AuthProvider, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AuthProvider authProvider) {
        return authProvider.getCode();
    }

    @Override
    public AuthProvider convertToEntityAttribute(Integer code) {
        return AuthProvider.of(code);
    }
}
