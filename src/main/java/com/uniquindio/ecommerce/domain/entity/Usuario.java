package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.Rol;
import java.util.Locale;
import java.util.UUID;
import java.util.Objects;

/** Cuenta de acceso, separada del perfil de creador con taller y vitrinas. */
public final class Usuario {
    private final UUID id;
    private final String nombre;
    private final String correo;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;

    private Usuario(UUID id, String nombre, String correo, String passwordHash, Rol rol, boolean activo) {
        this.id = Objects.requireNonNull(id);
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100)
            throw new IllegalArgumentException("Nombre inválido.");
        this.nombre = nombre.trim();
        this.correo = normalizarCorreo(correo);
        if (passwordHash == null || !passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))
            throw new IllegalArgumentException("La contraseña debe estar codificada con BCrypt.");
        this.passwordHash = passwordHash;
        this.rol = Objects.requireNonNull(rol);
        this.activo = activo;
    }

    public static Usuario reconstruir(UUID id, String nombre, String correo, String hash, Rol rol, boolean activo) {
        return new Usuario(id, nombre, correo, hash, rol, activo);
    }
    public static String normalizarCorreo(String correo) {
        if (correo == null || correo.trim().length() > 254 || !correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Correo inválido.");
        return correo.trim().toLowerCase(Locale.ROOT);
    }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
}
