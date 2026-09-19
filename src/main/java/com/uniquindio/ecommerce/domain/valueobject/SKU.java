package com.uniquindio.ecommerce.domain.valueobject;
import java.util.Objects;

public record SKU(String value) {

    public SKU {
        Objects.requireNonNull(value, "El SKU no puede ser nulo.");

        if (value.isBlank()) {
            throw new IllegalArgumentException("El SKU no puede estar vacío o en blanco.");
        }

        // Limpiamos espacios y estandarizamos a mayúsculas por consistencia
        // Nota: en los records de Java, puedes reasignar los parámetros en el constructor compacto
        value = value.trim().toUpperCase();
    }
}
}
