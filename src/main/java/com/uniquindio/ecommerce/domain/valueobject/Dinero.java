package com.uniquindio.ecommerce.domain.valueobject;

import java.math.BigDecimal;

public record Dinero(BigDecimal monto, String moneda) {
    public Dinero {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto del dinero no puede ser negativo.");
        }
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria.");
        }
    }
}
