package com.uniquindio.ecommerce.domain.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Creador {

    private final UUID id;
    private final String nombre;
    private final String email;
    private final String telefono;
    private final String taller;
    private final boolean cuentaValidada;
    private final List<VitrinaDeCreador> vitrinas;

    public Creador(UUID id, String nombre, String email, String telefono, String taller, boolean cuentaValidada, List<VitrinaDeCreador> vitrinas) {
        this.id = Objects.requireNonNull(id, "El ID del creador es obligatorio.");
        this.nombre = validarNombre(nombre);
        this.email = validarEmail(email);
        this.telefono = Objects.requireNonNull(telefono, "El teléfono es obligatorio.");
        this.taller = Objects.requireNonNull(taller, "El taller es obligatorio.");
        this.cuentaValidada = cuentaValidada;
        this.vitrinas = validarVitrinas(vitrinas);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        return nombre.trim();
    }

    private String validarEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido.");
        }
        return email.trim();
    }

    private List<VitrinaDeCreador> validarVitrinas(List<VitrinaDeCreador> vitrinas) {
        if (vitrinas == null || vitrinas.isEmpty()) {
            throw new IllegalArgumentException("El creador debe tener al menos una vitrina activa.");
        }
        return List.copyOf(vitrinas);
    }

    public void agregarVitrina(VitrinaDeCreador vitrina) {
        Objects.requireNonNull(vitrina, "La vitrina no puede ser nula.");
        vitrinas.add(vitrina);
    }

    public void eliminarVitrina(UUID vitrinaId) {
        vitrinas.removeIf(vitrina -> vitrina.getId().equals(vitrinaId));
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getTaller() {
        return taller;
    }

    public boolean isCuentaValidada() {
        return cuentaValidada;
    }

    public List<VitrinaDeCreador> getVitrinas() {
        return vitrinas;
    }
}