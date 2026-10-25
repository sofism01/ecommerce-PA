package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VitrinaDeCreador {

    private final UUID id;
    private final UUID creadorId;
    private final String nombreMarca;
    private final String descripcionMarca;
    private final List<PrendaDeAutor> catalogo;

    private VitrinaDeCreador(UUID id, UUID creadorId, String nombreMarca, String descripcionMarca, List<PrendaDeAutor> catalogo) {
        if (id == null) {
            throw new ReglaDominioException("El ID de la vitrina es obligatorio.");
        }
        this.id = id;
        if (creadorId == null) {
            throw new ReglaDominioException("El ID del creador es obligatorio.");
        }
        this.creadorId = creadorId;
        this.nombreMarca = validarNombre(nombreMarca);
        if (descripcionMarca == null) {
            throw new ReglaDominioException("La descripción de la marca es obligatoria.");
        }
        this.descripcionMarca = descripcionMarca;
        if (catalogo == null) {
            throw new ReglaDominioException("El catálogo no puede ser nulo.");
        }
        if (catalogo.stream().anyMatch(Objects::isNull)) {
            throw new ReglaDominioException("El catálogo no puede contener prendas nulas.");
        }
        this.catalogo = new ArrayList<>(catalogo);
    }

    public static VitrinaDeCreador crear(UUID id, UUID creadorId, String nombreMarca, String descripcionMarca, List<PrendaDeAutor> catalogo) {
        return new VitrinaDeCreador(id, creadorId, nombreMarca, descripcionMarca, catalogo);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la marca no puede estar vacío.");
        }
        return nombre.trim();
    }

    public void agregarPrenda(PrendaDeAutor prenda) {
        if (prenda == null) {
            throw new ReglaDominioException("La prenda no puede ser nula.");
        }
        catalogo.add(prenda);
    }

    public void eliminarPrenda(UUID prendaId) {
        if (prendaId == null) {
            throw new ReglaDominioException("El ID de la prenda es obligatorio.");
        }
        catalogo.removeIf(prenda -> prenda.getId().value().equals(prendaId));
    }

    // --- equals() y hashCode() basados en el id ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VitrinaDeCreador that = (VitrinaDeCreador) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getCreadorId() {
        return creadorId;
    }

    public String getNombreMarca() {
        return nombreMarca;
    }

    public String getDescripcionMarca() {
        return descripcionMarca;
    }

    public List<PrendaDeAutor> getCatalogo() {
        return List.copyOf(catalogo);
    }
}
