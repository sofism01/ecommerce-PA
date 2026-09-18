package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import java.util.UUID;

public class LineaDeBolsa {

    private final UUID varianteId;
    private final int cantidad;
    private final Dinero precioUnitario;

    public LineaDeBolsa(UUID varianteId, int cantidad, Dinero precioUnitario) {
        if (varianteId == null) {
            throw new IllegalArgumentException("El identificador de la variante de talla es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad en la línea de bolsa debe ser mayor a cero.");
        }
        if (precioUnitario == null) {
            throw new IllegalArgumentException("El precio unitario es obligatorio.");
        }
        this.varianteId = varianteId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public LineaDeBolsa actualizarCantidad(int nuevaCantidad) {
        return new LineaDeBolsa(this.varianteId, nuevaCantidad, this.precioUnitario);
    }

    public Dinero calcularSubtotal() {
        return new Dinero(
                precioUnitario.monto().multiply(new java.math.BigDecimal(cantidad)),
                precioUnitario.moneda()
        );
    }

    public UUID varianteId() {
        return varianteId;
    }

    public int cantidad() {
        return cantidad;
    }

    public Dinero precioUnitario() {
        return precioUnitario;
    }
}
