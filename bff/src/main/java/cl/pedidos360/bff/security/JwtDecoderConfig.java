package cl.pedidos360.bff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

// Construye el JwtDecoder que valida el token recibido desde el IDaaS (SDD §22):
// - Firma: descarga las claves públicas (JWKS) del issuer y verifica la firma.
// - Issuer: el emisor debe ser el tenant configurado.
// - Vigencia: exp y nbf (JwtTimestampValidator, incluido por defecto).
// - Audience: el token debe estar dirigido a esta API.
@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${security.jwt.audience}")
    private String audience;

    @Bean
    public NimbusJwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder =
            (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        // createDefaultWithIssuer valida issuer + vigencia (exp/nbf).
        OAuth2TokenValidator<Jwt> withIssuerAndTimestamp =
            JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience =
            new DelegatingOAuth2TokenValidator<>(
                withIssuerAndTimestamp, new AudienceValidator(audience));

        decoder.setJwtValidator(withAudience);
        return decoder;
    }
}
