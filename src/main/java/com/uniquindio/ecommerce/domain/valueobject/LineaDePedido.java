package com.uniquindio.ecommerce.domain.valueobject;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import java.math.BigDecimal;
import java.util.UUID;

/** Instantánea del artículo comprado; no depende del estado futuro del catálogo. */
public record LineaDePedido(UUID prendaId, UUID varianteId, String nombrePrenda,
        SKU sku, TipoTalla tipoTalla, int cantidad, Dinero precioUnitario) {
    public LineaDePedido {
        if (prendaId == null || varianteId == null || nombrePrenda == null || nombrePrenda.isBlank()
                || sku == null || tipoTalla == null || cantidad <= 0 || precioUnitario == null
                || precioUnitario.monto().signum() <= 0)
            throw new ReglaDominioException("Los datos históricos de la línea del pedido son inválidos.");
    }
    public Dinero calcularSubtotal() {
        return new Dinero(precioUnitario.monto().multiply(BigDecimal.valueOf(cantidad)), precioUnitario.moneda());
    }
}
