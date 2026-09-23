package cl.pedidos360.bff.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

// Valida que el claim "aud" del JWT corresponda a esta API (SDD §10, §22).
// Acepta cualquiera de las audiencias válidas: los tokens v1 traen
// "api://<client-id>" y los v2 traen "<client-id>" (sin prefijo).
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> accepted;

    public AudienceValidator(List<String> accepted) {
        this.accepted = accepted;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(accepted::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "La audiencia (aud) del token no es válida", null)
        );
    }
}
