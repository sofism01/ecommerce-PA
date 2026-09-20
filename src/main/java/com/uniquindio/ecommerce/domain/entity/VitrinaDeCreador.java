package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VitrinaDeCreador {

    private final UUID id;
    private final UUID creadorId;
    private final String nombreMarca;
    private final String descripcionMarca;
    private final List<PrendaDeAutor> catalogo;

    public VitrinaDeCreador(UUID id, UUID creadorId, String nombreMarca, String descripcionMarca, List<PrendaDeAutor> catalogo) {
        this.id = Objects.requireNonNull(id, "El ID de la vitrina es obligatorio.");
        this.creadorId = Objects.requireNonNull(creadorId, "El ID del creador es obligatorio.");
        this.nombreMarca = validarNombre(nombreMarca);
        this.descripcionMarca = Objects.requireNonNull(descripcionMarca, "La descripción de la marca es obligatoria.");
        this.catalogo = Objects.requireNonNull(catalogo, "El catálogo no puede ser nulo.");
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la marca no puede estar vacío.");
        }
        return nombre.trim();
    }

    public void agregarPrenda(PrendaDeAutor prenda) {
        Objects.requireNonNull(prenda, "La prenda no puede ser nula.");
        catalogo.add(prenda);
    }

    public void eliminarPrenda(UUID prendaId) {
        catalogo.removeIf(prenda -> prenda.getId().equals(prendaId));
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
        return catalogo;
    }
}