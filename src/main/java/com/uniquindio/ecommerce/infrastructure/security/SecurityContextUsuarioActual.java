package com.uniquindio.ecommerce.infrastructure.security;
import com.uniquindio.ecommerce.application.security.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class SecurityContextUsuarioActual implements UsuarioActual {
    public UUID id() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
            throw new CredencialesInvalidasException();
        return UUID.fromString(jwt.getSubject());
    }
}
