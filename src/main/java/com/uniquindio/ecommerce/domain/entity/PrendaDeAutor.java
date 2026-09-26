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
    private final UUID creadorId;
    private final String nombre;
    private final String descripcion;
    private final Dinero precioBase;
    private final List<VariantePrenda> variantes;
    private final boolean disponibleParaLaVenta;

    private PrendaDeAutor(PrendaDeAutorId id, UUID creadorId, String nombre, String descripcion, Dinero precioBase, List<VariantePrenda> variantes, boolean disponibleParaLaVenta) {
        this.id = Objects.requireNonNull(id, "El ID de la prenda es obligatorio.");
        this.creadorId = Objects.requireNonNull(creadorId, "El ID del creador es obligatorio.");
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion != null ? descripcion.trim() : "";
        this.precioBase = validarPrecio(precioBase);
        this.variantes = validarVariantes(variantes);
        this.disponibleParaLaVenta = disponibleParaLaVenta;
    }

    public static PrendaDeAutor crear(PrendaDeAutorId id, UUID creadorId, String nombre, String descripcion, Dinero precioBase, List<VariantePrenda> variantes) {
        return new PrendaDeAutor(id, creadorId, nombre, descripcion, precioBase, variantes, true);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la prenda no puede estar vacío.");
        }
        return nombre.trim();
    }

    private Dinero validarPrecio(Dinero precioBase) {
        if (precioBase == null || precioBase.monto().signum() <= 0) {
            throw new IllegalArgumentException("El precio base de la prenda debe ser mayor a cero.");
        }
        return precioBase;
    }

    private List<VariantePrenda> validarVariantes(List<VariantePrenda> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw new IllegalArgumentException("Una prenda de autor debe tener al menos una variante de talla.");
        }
        return List.copyOf(variantes);
    }

    // --- equals() y hashCode() por identidad (basados únicamente en el id) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrendaDeAutor that = (PrendaDeAutor) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- Getters ---

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