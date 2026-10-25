package com.uniquindio.ecommerce.application.dto.response;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import java.util.UUID;
public record UsuarioResponse(UUID id, String nombre, String correo, Rol rol, boolean activo) {}
