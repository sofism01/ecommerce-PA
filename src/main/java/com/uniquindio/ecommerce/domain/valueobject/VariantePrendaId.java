package com.uniquindio.ecommerce.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public class VariantePrendaId {

    private final UUID id;

    public VariantePrendaId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la variante no puede ser nulo.");
        }
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantePrendaId that = (VariantePrendaId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VariantePrendaId{" +
                "id=" + id +
                '}';
    }
}
