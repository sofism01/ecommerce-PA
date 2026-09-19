package com.uniquindio.ecommerce.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record PrendaDeAutorId(UUID value) {
    public PrendaDeAutorId {
        Objects.requireNonNull(value, "El identificador de la prenda de autor no puede ser nulo.");
    }

    public static PrendaDeAutorId random() {
        return new PrendaDeAutorId(UUID.randomUUID());
    }
}
