package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.domain.valueobject.VariantePrendaId;

import java.util.ArrayList;
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


    private int stockTotal;

    private PrendaDeAutor(PrendaDeAutorId id, UUID creadorId, String nombre, String descripcion, Dinero precioBase, List<VariantePrenda> variantes, boolean disponibleParaLaVenta, int stockTotal) {
        if (id == null) {
            throw new ReglaDominioException("El ID de la prenda es obligatorio.");
        }
        this.id = id;
        if (creadorId == null) {
            throw new ReglaDominioException("El ID del creador es obligatorio.");
        }
        this.creadorId = creadorId;
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion != null ? descripcion.trim() : "";
        this.precioBase = validarPrecio(precioBase);
        this.variantes = validarVariantes(variantes);
        this.disponibleParaLaVenta = disponibleParaLaVenta;
        this.stockTotal = validarStock(stockTotal);
    }

    private List<VariantePrenda> validarVariantes(List<VariantePrenda> variantes) {
        if (variantes == null) {
            throw new ReglaDominioException("La lista de variantes no puede ser nula.");
        }
        if (variantes.isEmpty()) {
            throw new ReglaDominioException("Una prenda de autor debe tener al menos una variante de talla.");
        }
        if (variantes.stream().anyMatch(Objects::isNull)) {
            throw new ReglaDominioException("Las variantes no pueden contener elementos nulos.");
        }
        if (variantes.stream().distinct().count() != variantes.size()) {
            throw new ReglaDominioException("La lista de variantes contiene elementos duplicados.");
        }
        return new ArrayList<>(variantes);
    }

    public static PrendaDeAutor crear(PrendaDeAutorId id, UUID creadorId, String nombre, String descripcion, Dinero precioBase, List<VariantePrenda> variantes, int stockTotal) {
        return new PrendaDeAutor(id, creadorId, nombre, descripcion, precioBase, variantes, true, stockTotal);
    }

    /** Reconstruye un estado existente aplicando las mismas invariantes de creación. */
    public static PrendaDeAutor reconstruir(PrendaDeAutorId id, UUID creadorId, String nombre,
            String descripcion, Dinero precioBase, List<VariantePrenda> variantes,
            boolean disponibleParaLaVenta, int stockTotal) {
        return new PrendaDeAutor(id, creadorId, nombre, descripcion, precioBase, variantes, disponibleParaLaVenta, stockTotal);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la prenda no puede estar vacío.");
        }
        return nombre.trim();
    }

    private Dinero validarPrecio(Dinero precioBase) {
        if (precioBase == null || precioBase.monto().signum() <= 0) {
            throw new ReglaDominioException("El precio base de la prenda debe ser mayor a cero.");
        }
        return precioBase;
    }

    private int validarStock(int stock) {
        if (stock < 0) {
            throw new ReglaDominioException("El stock no puede ser negativo.");
        }
        return stock;
    }

    public void actualizarStock(int nuevoStock) {
        this.stockTotal = validarStock(nuevoStock);
    }

    public int getStockTotal() {
        return stockTotal;
    }

    public PrendaDeAutor actualizarPrecio(Dinero nuevoPrecio) {
        return new PrendaDeAutor(this.id, this.creadorId, this.nombre, this.descripcion, nuevoPrecio, this.variantes, this.disponibleParaLaVenta, this.stockTotal);
    }

    public void actualizarStockVariante(VariantePrendaId varianteId, int nuevoStock) {
        if (varianteId == null) {
            throw new ReglaDominioException("El ID de la variante es obligatorio.");
        }
        validarStock(nuevoStock);
        for (int i = 0; i < variantes.size(); i++) {
            VariantePrenda variante = variantes.get(i);
            if (variante.getId().equals(varianteId.getId())) {
                variantes.set(i, variante.actualizarStock(nuevoStock));
                return;
            }
        }
        throw new RecursoNoEncontradoException("La variante especificada no existe.");
    }

    public PrendaDeAutor suspenderVenta() {
        if (!this.disponibleParaLaVenta) {
            throw new ReglaDominioException("La prenda ya está suspendida para la venta.");
        }
        return new PrendaDeAutor(this.id, this.creadorId, this.nombre, this.descripcion, this.precioBase, this.variantes, false, this.stockTotal);
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
