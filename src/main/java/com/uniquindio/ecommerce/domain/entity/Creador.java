package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;

import java.util.ArrayList;
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

    private Creador(UUID id, String nombre, String email, String telefono, String taller, boolean cuentaValidada, List<VitrinaDeCreador> vitrinas) {
        if (id == null) {
            throw new ReglaDominioException("El ID del creador es obligatorio.");
        }
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.email = validarEmail(email);
        if (telefono == null || telefono.isBlank()) {
            throw new ReglaDominioException("El teléfono es obligatorio.");
        }
        this.telefono = telefono.trim();
        if (taller == null || taller.isBlank()) {
            throw new ReglaDominioException("El taller es obligatorio.");
        }
        this.taller = taller.trim();
        this.cuentaValidada = cuentaValidada;
        this.vitrinas = validarVitrinas(vitrinas);
    }

    public static Creador crear(UUID id, String nombre, String email, String telefono, String taller, boolean cuentaValidada, List<VitrinaDeCreador> vitrinas) {
        return new Creador(id, nombre, email, telefono, taller, cuentaValidada, vitrinas);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre no puede estar vacío.");
        }
        return nombre.trim();
    }

    private String validarEmail(String email) {
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+$")) {
            throw new ReglaDominioException("El email no es válido.");
        }
        return email.trim();
    }

    private List<VitrinaDeCreador> validarVitrinas(List<VitrinaDeCreador> vitrinas) {
        if (vitrinas == null || vitrinas.isEmpty()) {
            throw new ReglaDominioException("El creador debe tener al menos una vitrina activa.");
        }
        if (vitrinas.stream().anyMatch(Objects::isNull)) {
            throw new ReglaDominioException("Las vitrinas no pueden contener elementos nulos.");
        }
        return new ArrayList<>(vitrinas);
    }

    public void agregarVitrina(VitrinaDeCreador vitrina) {
        if (vitrina == null) {
            throw new ReglaDominioException("La vitrina no puede ser nula.");
        }
        vitrinas.add(vitrina);
    }

    public void eliminarVitrina(UUID vitrinaId) {
        if (vitrinaId == null) {
            throw new ReglaDominioException("El ID de la vitrina es obligatorio.");
        }
        long restantes = vitrinas.stream().filter(vitrina -> !vitrina.getId().equals(vitrinaId)).count();
        if (restantes == 0) {
            throw new ReglaDominioException("El creador debe tener al menos una vitrina activa.");
        }
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
        return List.copyOf(vitrinas);
    }

    // --- equals y hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Creador creador = (Creador) o;
        return Objects.equals(id, creador.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
