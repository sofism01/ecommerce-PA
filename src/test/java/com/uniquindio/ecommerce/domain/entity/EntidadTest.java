package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntidadTest {

    @Test
    void igualdadPorIdentidad_PrendaDeAutor() {
        // Arrange
        PrendaDeAutorId id = new PrendaDeAutorId(UUID.randomUUID());
        VariantePrenda variante = new VariantePrenda(UUID.randomUUID(), new SKU("SKU123"), TipoTalla.M, 10);
        PrendaDeAutor prenda1 = new PrendaDeAutor(id, UUID.randomUUID(), "Camisa", "Camisa de algodón",
                new Dinero(new BigDecimal("50.00"), "USD"), List.of(variante));
        PrendaDeAutor prenda2 = new PrendaDeAutor(id, UUID.randomUUID(), "Pantalón", "Pantalón de lino",
                new Dinero(new BigDecimal("70.00"), "USD"), List.of(variante));

        // Act & Assert
        assertEquals(prenda1, prenda2, "Dos prendas con el mismo ID deben ser iguales, aunque tengan datos distintos.");
    }

    @Test
    void reglaProtegida_ActualizarPrecioPrenda() {
        // Arrange
        VariantePrenda variante = new VariantePrenda(UUID.randomUUID(), new SKU("SKU123"), TipoTalla.M, 10);
        PrendaDeAutor prenda = new PrendaDeAutor(new PrendaDeAutorId(UUID.randomUUID()), UUID.randomUUID(),
                "Camisa", "Camisa de algodón", new Dinero(new BigDecimal("50.00"), "USD"), List.of(variante));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                prenda.actualizarPrecio(new Dinero(new BigDecimal("-10.00"), "USD"))
        );
        assertEquals("El monto del dinero no puede ser negativo.", exception.getMessage());
    }

    @Test
    void reglaProtegida_ValidarVitrinasCreador() {
        //Asegura que no se pueda crear un Creador sin al menos una vitrina activa
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new Creador(id, "Juan", "juan@example.com", "123456789", "Taller 1", true, List.of())
        );
        assertEquals("El creador debe tener al menos una vitrina activa.", exception.getMessage());
    }
}
