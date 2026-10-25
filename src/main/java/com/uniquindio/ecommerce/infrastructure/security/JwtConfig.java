package com.uniquindio.ecommerce.infrastructure.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.*;
@Configuration
public class JwtConfig {
    @Bean
    SecretKey jwtKey(@Value("${boheme.jwt.secret}") String secret) {
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("BOHEME_JWT_SECRET debe ser Base64 válido."); }
        if (bytes.length < 32) throw new IllegalStateException("BOHEME_JWT_SECRET requiere al menos 32 bytes aleatorios.");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean
    Duration jwtDuration(@Value("${boheme.jwt.duration}") Duration duration) {
        if (duration.isNegative() || duration.isZero() || duration.toSeconds() < 1 || duration.compareTo(Duration.ofHours(24)) > 0)
            throw new IllegalStateException("La duración JWT debe estar entre 1 segundo y 24 horas.");
        return duration;
    }
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return NimbusJwtEncoder.withSecretKey(jwtKey).algorithm(MacAlgorithm.HS256).build();
    }
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> required = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                        || jwt.getIssuedAt().isAfter(Instant.now()) || jwt.getClaimAsString("correo") == null
                        || !List.of("COMPRADOR","VENDEDOR").contains(jwt.getClaimAsString("rol")))
                    throw new IllegalArgumentException();
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException e) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Token inválido.",null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator("boheme"), required));
        return decoder;
    }
}
