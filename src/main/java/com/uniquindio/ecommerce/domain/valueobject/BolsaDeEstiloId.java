package com.uniquindio.ecommerce.domain.valueobject;

import java.util.UUID;

public record BolsaDeEstiloId(UUID valor) {
    public BolsaDeEstiloId {
        if (valor == null) {
            throw new IllegalArgumentException("El identificador de la bolsa de estilo no puede ser nulo.");
        }
    }
}
