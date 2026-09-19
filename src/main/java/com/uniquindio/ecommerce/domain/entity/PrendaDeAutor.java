package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.domain.valueobject.VariantePrenda;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PrendaDeAutor {

    private final PrendaDeAutorId id;
    private final UUID creadorId; // Identificador del taller o diseñador
    private String nombre;
    private String descripcion;
    private Dinero precioBase;
    private final List<VariantePrenda> variantes;
    private boolean disponibleParaLaVenta;

    public PrendaDeAutor(PrendaDeAutorId id, UUID creadorId, String nombre, String descripcion, Dinero precioBase, List<VariantePrenda> variantes) {
        this.id = Objects.requireNonNull(id, "El ID de la prenda es obligatorio.");
        this.creadorId = Objects.requireNonNull(creadorId, "El ID del creador es obligatorio.");

        actualizarNombre(nombre);
        actualizarDescripcion(descripcion);
        actualizarPrecio(precioBase);

        this.variantes = new java.util.ArrayList<>(Objects.requireNonNull(variantes, "Las variantes de la prenda son obligatorias."));
        if (this.variantes.isEmpty()) {
            throw new IllegalArgumentException("Una prenda de autor debe tener al menos una variante de talla.");
        }

        this.disponibleParaLaVenta = true;
    }

    public void actualizarPrecio(Dinero nuevoPrecio) {
        this.precioBase = Objects.requireNonNull(nuevoPrecio, "El precio base no puede ser nulo.");
        if (nuevoPrecio.monto().signum() <= 0) {
            throw new IllegalArgumentException("El precio base de la prenda debe ser mayor a cero.");
        }
    }

    public void actualizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la prenda no puede estar vacío.");
        }
        this.nombre = nombre.trim();
    }

    public void actualizarDescripcion(String descripcion) {
        this.descripcion = descripcion != null ? descripcion.trim() : "";
    }

    public void suspenderVenta() {
        this.disponibleParaLaVenta = false;
    }

    public void habilitarVenta() {
        this.disponibleParaLaVenta = true;
    }

    // Getters defensivos (sin setters públicos para proteger el estado)
    public PrendaDeAutorId getId() {
        return id;
    }

    public UUID getCreadorId() {
        return creadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Dinero getPrecioBase() {
        return precioBase;
    }

    public List<VariantePrenda> getVariantes() {
        return Collections.unmodifiableList(variantes);
    }

    public boolean isDisponibleParaLaVenta() {
        return disponibleParaLaVenta;
    }
}