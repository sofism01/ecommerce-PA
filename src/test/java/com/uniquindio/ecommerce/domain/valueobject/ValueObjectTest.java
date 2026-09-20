package com.uniquindio.ecommerce.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectTest {

    @Test
    void igualdadPorValor_Dinero() {
        // Valida que dos instancias de Dinero con el mismo monto y moneda sean consideradas iguales.
        // Arrange
        Dinero dinero1 = new Dinero(new BigDecimal("100.00"), "USD");
        Dinero dinero2 = new Dinero(new BigDecimal("100.00"), "USD");

        // Act & Assert
        assertEquals(dinero1, dinero2, "Los objetos Dinero con el mismo valor deben ser iguales.");
    }

    @Test
    void validacionMontoNegativo_Dinero() {
        // Valida que no se permita crear un objeto Dinero con un monto negativo.
        // Arrange
        BigDecimal montoNegativo = new BigDecimal("-10.00");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new Dinero(montoNegativo, "USD")
        );
        assertEquals("El monto del dinero no puede ser negativo.", exception.getMessage());
    }

    @Test
    void igualdadPorValor_SKU() {
        // Valida que dos instancias de SKU con el mismo valor sean consideradas iguales.
        // Arrange
        SKU sku1 = new SKU("ABC123");
        SKU sku2 = new SKU("ABC123");

        // Act & Assert
        assertEquals(sku1, sku2, "Los objetos SKU con el mismo valor deben ser iguales.");
    }

    @Test
    void validacionSKUVacio() {
        // Valida que no se permita crear un objeto SKU con un valor vacío o en blanco,
        // Arrange
        String skuVacio = "   ";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SKU(skuVacio)
        );
        assertEquals("El SKU no puede estar vacío o en blanco.", exception.getMessage());
    }
}
