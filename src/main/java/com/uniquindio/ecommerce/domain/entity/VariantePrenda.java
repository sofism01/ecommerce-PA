package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;

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
        if (id == null) {
            throw new ReglaDominioException("El ID de la variante es obligatorio.");
        }
        this.id = id;
        if (sku == null) {
            throw new ReglaDominioException("El SKU es obligatorio.");
        }
        this.sku = sku;
        if (tipoTalla == null) {
            throw new ReglaDominioException("El tipo de talla es obligatorio.");
        }
        this.tipoTalla = tipoTalla;

        if (stockDisponible < 0) {
            throw new ReglaDominioException("El stock disponible no puede ser negativo.");
        }
        this.stockDisponible = stockDisponible;
    }

    public static VariantePrenda crear(UUID id, SKU sku, TipoTalla tipoTalla, int stockDisponible) {
        return new VariantePrenda(id, sku, tipoTalla, stockDisponible);
    }

    public VariantePrenda reducirStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad a reducir debe ser mayor a cero.");
        }
        if (this.stockDisponible < cantidad) {
            throw new com.uniquindio.ecommerce.domain.exception.StockInsuficienteException("Stock insuficiente para la variante con SKU: " + sku.value());
        }
        return new VariantePrenda(this.id, this.sku, this.tipoTalla, this.stockDisponible - cantidad);
    }

    public VariantePrenda actualizarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new ReglaDominioException("El stock no puede ser negativo.");
        }
        return new VariantePrenda(this.id, this.sku, this.tipoTalla, nuevoStock);
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

    public UUID getId() {
        return id;
    }

    public SKU getSku() {
        return sku;
    }

    public TipoTalla getTipoTalla() {
        return tipoTalla;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }
}
