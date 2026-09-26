package com.uniquindio.ecommerce.domain.valueobject;
import java.util.Objects;

public record SKU(String value) {
// Stock Keeping Unit: identificador único para un producto o variante de producto en el sistema de inventario
    public SKU {
        Objects.requireNonNull(value, "El SKU no puede ser nulo.");

        if (value.isBlank()) {
            throw new IllegalArgumentException("El SKU no puede estar vacío o en blanco.");
        }

        // Limpiamos espacios y estandarizamos a mayúsculas
        value = value.trim().toUpperCase();
    }
}

