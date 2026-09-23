package cl.pedidos360.bff.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

// JwtDecoder que valida el token del IDaaS (SDD §22):
// - Firma: verifica contra el JWKS del tenant de Entra ID.
// - Issuer: acepta el emisor v2 (login.microsoftonline.com/<tid>/v2.0) y el v1
//   (sts.windows.net/<tid>/), porque Azure emite v1 por defecto para APIs.
// - Vigencia: exp y nbf.
// - Audience: el token debe estar dirigido a esta API.
@Configuration
public class JwtDecoderConfig {

    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${security.jwt.audience}")
    private String audience;

    @Value("${azure.client-id}")
    private String clientId;

    @Bean
    public NimbusJwtDecoder jwtDecoder() {
        // El JWKS v2 del tenant sirve las claves usadas para firmar ambos tipos de token.
        String jwkSetUri =
            "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        List<String> validIssuers = List.of(
            "https://login.microsoftonline.com/" + tenantId + "/v2.0",
            "https://sts.windows.net/" + tenantId + "/"
        );

        // Acepta la audiencia configurada y ambas formas del client-id
        // (v1: "api://<id>", v2: "<id>").
        List<String> validAudiences = List.of(audience, clientId, "api://" + clientId)
            .stream().distinct().toList();

        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            issuerValidator(validIssuers),
            new AudienceValidator(validAudiences)
        );
        decoder.setJwtValidator(validators);
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> issuerValidator(List<String> validIssuers) {
        return jwt -> {
            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            if (iss != null && validIssuers.contains(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token",
                    "El issuer del token no es válido: " + iss, null));
        };
    }
}
