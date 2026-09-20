package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntidadTest {

    @Test
    void igualdadPorIdentidad_PrendaDeAutor() {
        //Valida que dos instancias de la entidad PrendaDeAutor con el mismo identificador (id) sean consideradas iguales
        // Arrange
        PrendaDeAutorId id = new PrendaDeAutorId(UUID.randomUUID());
        PrendaDeAutor prenda1 = new PrendaDeAutor(id, UUID.randomUUID(), "Camisa", "Camisa de algodón",
                new Dinero(new BigDecimal("50.00"), "USD"), List.of());
        PrendaDeAutor prenda2 = new PrendaDeAutor(id, UUID.randomUUID(), "Pantalón", "Pantalón de lino",
                new Dinero(new BigDecimal("70.00"), "USD"), List.of());

        // Act & Assert
        assertEquals(prenda1, prenda2, "Dos prendas con el mismo ID deben ser iguales, aunque tengan datos distintos.");
    }

    @Test
    void reglaProtegida_ActualizarPrecioPrenda() {
        //Verifica que no se permita actualizar el precio de una PrendaDeAutor con un valor negativo
        // Arrange
        PrendaDeAutor prenda = new PrendaDeAutor(new PrendaDeAutorId(UUID.randomUUID()), UUID.randomUUID(),
                "Camisa", "Camisa de algodón", new Dinero(new BigDecimal("50.00"), "USD"), List.of());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                prenda.actualizarPrecio(new Dinero(new BigDecimal("-10.00"), "USD"))
        );
        assertEquals("El precio base de la prenda debe ser mayor a cero.", exception.getMessage());
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
