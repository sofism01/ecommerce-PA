package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.SKU;
import com.uniquindio.ecommerce.domain.valueobject.Dinero;

import java.util.Objects;

public class VarianteDeTalla {

    private final SKU sku;
    private final String talla; // Ejemplo: S, M, L, XL
    private final String color; // Ejemplo: Rojo, Azul, Negro
    private int stock;
    private final Dinero precio;

    public VarianteDeTalla(SKU sku, String talla, String color, int stock, Dinero precio) {
        this.sku = Objects.requireNonNull(sku, "El SKU es obligatorio.");
        this.talla = validarTalla(talla);
        this.color = validarColor(color);
        this.precio = Objects.requireNonNull(precio, "El precio es obligatorio.");
        actualizarStock(stock);
    }

    private String validarTalla(String talla) {
        if (talla == null || talla.isBlank()) {
            throw new IllegalArgumentException("La talla no puede estar vacía.");
        }
        return talla.trim().toUpperCase();
    }

    private String validarColor(String color) {
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("El color no puede estar vacío.");
        }
        return color.trim();
    }

    public void actualizarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        this.stock = nuevoStock;
    }

    public boolean reducirStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reducir debe ser mayor a cero.");
        }
        if (cantidad > stock) {
            return false; // No hay suficiente stock
        }
        this.stock -= cantidad;
        return true;
    }

    // --- equals() y hashCode() basados en el SKU ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarianteDeTalla that = (VarianteDeTalla) o;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    // --- Getters ---

    public SKU getSku() {
        return sku;
    }

    public String getTalla() {
        return talla;
    }

    public String getColor() {
        return color;
    }

    public int getStock() {
        return stock;
    }

    public Dinero getPrecio() {
        return precio;
    }
}