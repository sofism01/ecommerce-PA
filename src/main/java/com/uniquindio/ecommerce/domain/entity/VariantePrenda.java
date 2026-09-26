package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.SKU;
import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;

import java.util.Objects;
import java.util.UUID;

public class VariantePrenda {

    private final UUID id;
    private final SKU sku;
    private final TipoTalla tipoTalla;
    private final int stockDisponible;

    private VariantePrenda(UUID id, SKU sku, TipoTalla tipoTalla, int stockDisponible) {
        this.id = Objects.requireNonNull(id, "El ID de la variante es obligatorio.");
        this.sku = Objects.requireNonNull(sku, "El SKU es obligatorio.");
        this.tipoTalla = Objects.requireNonNull(tipoTalla, "El tipo de talla es obligatorio.");

        if (stockDisponible < 0) {
            throw new IllegalArgumentException("El stock disponible no puede ser negativo.");
        }
        this.stockDisponible = stockDisponible;
    }

    public static VariantePrenda crear(UUID id, SKU sku, TipoTalla tipoTalla, int stockDisponible) {
        return new VariantePrenda(id, sku, tipoTalla, stockDisponible);
    }

    public VariantePrenda reducirStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reducir debe ser mayor a cero.");
        }
        if (this.stockDisponible < cantidad) {
            throw new com.uniquindio.ecommerce.domain.exception.StockInsuficienteException("Stock insuficiente para la variante con SKU: " + sku.value());
        }
        return new VariantePrenda(this.id, this.sku, this.tipoTalla, this.stockDisponible - cantidad);
    }

    public UUID id() { return id; }
    public SKU sku() { return sku; }
    public TipoTalla tipoTalla() { return tipoTalla; }
    public int stockDisponible() { return stockDisponible; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantePrenda that = (VariantePrenda) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
