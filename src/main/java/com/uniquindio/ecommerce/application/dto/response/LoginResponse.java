package com.uniquindio.ecommerce.application.dto.response;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import java.time.Instant;
public record LoginResponse(String token, String tokenType, long expiresIn, Instant expiresAt,
                            Rol rol, UsuarioResponse usuario) {}
