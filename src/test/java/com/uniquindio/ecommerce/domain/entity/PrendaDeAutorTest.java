package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.domain.valueobject.SKU;
import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PrendaDeAutorTest {
    @Test
    void crearPrenda_SinVariantes_LanzaExcepcion() {
        // Verifica que no se pueda crear una prenda de autor sin variantes de talla.
        // Arrange
        PrendaDeAutorId id = PrendaDeAutorId.random();
        UUID creadorId = UUID.randomUUID();
        Dinero precioBase = new Dinero(new BigDecimal("100.00"), "USD");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                PrendaDeAutor.crear(id, creadorId, "Camisa", "Camisa de algodón", precioBase, List.of())
        );
        assertEquals("Una prenda de autor debe tener al menos una variante de talla.", exception.getMessage());
    }

    @Test
    void actualizarPrecio_Valido_CambiaPrecio() {
        // Verifica que el precio base de una prenda de autor se actualice correctamente cuando el nuevo precio es válido.
        // Arrange
        VariantePrenda variante = VariantePrenda.crear(UUID.randomUUID(), new SKU("SKU123"), TipoTalla.M, 10);
        PrendaDeAutor prenda = PrendaDeAutor.crear(PrendaDeAutorId.random(), UUID.randomUUID(),
                "Camisa", "Camisa de algodón", new Dinero(new BigDecimal("50.00"), "USD"), List.of(variante));
        Dinero nuevoPrecio = new Dinero(new BigDecimal("75.00"), "USD");

        // Act
        prenda.actualizarPrecio(nuevoPrecio);

        // Assert
        assertEquals(nuevoPrecio, prenda.getPrecioBase(), "El precio base de la prenda debe actualizarse correctamente.");
    }
}
