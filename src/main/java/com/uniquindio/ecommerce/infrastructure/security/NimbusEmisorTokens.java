package com.uniquindio.ecommerce.infrastructure.security;
import com.uniquindio.ecommerce.application.security.EmisorTokens;
import com.uniquindio.ecommerce.application.dto.response.LoginResponse;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.UsuarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class NimbusEmisorTokens implements EmisorTokens {
    private final JwtEncoder encoder;
    private final Duration jwtDuration;
    private final UsuarioMapper mapper;
    public LoginResponse emitir(Usuario usuario) {
        // NumericDate de JWT tiene precisión de segundos; el DTO devuelve el mismo vencimiento.
        var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var expires=now.plusSeconds(jwtDuration.toSeconds());
        var claims=JwtClaimsSet.builder().issuer("boheme").subject(usuario.getId().toString())
                .issuedAt(now).expiresAt(expires).id(UUID.randomUUID().toString())
                .claim("correo",usuario.getCorreo()).claim("rol",usuario.getRol().name()).build();
        var token=encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),claims));
        return new LoginResponse(token.getTokenValue(),"Bearer",jwtDuration.toSeconds(),expires,
                usuario.getRol(),mapper.toResponse(usuario));
    }
}
