package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.StockInsuficienteException;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.domain.valueobject.LineaDeBolsa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BolsaDeEstiloTest {

    @Test
    void agregarLinea_StockInsuficiente_NoCambiaEstado() {
        // Valida que no se pueda agregar una línea si el stock es insuficiente y que el estado no cambie.
        // Arrange
        BolsaDeEstilo bolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), UUID.randomUUID());
        LineaDeBolsa nuevaLinea = new LineaDeBolsa(UUID.randomUUID(), 10, new Dinero(new BigDecimal("50.00"), "USD"));
        int stockDisponible = 5;

        // Act & Assert
        Exception exception = assertThrows(StockInsuficienteException.class, () ->
                bolsa.agregarLinea(nuevaLinea, stockDisponible)
        );
        assertEquals("No hay suficiente stock en el taller para esta variante de talla.", exception.getMessage());
        assertTrue(bolsa.getLineas().isEmpty(), "La bolsa no debe contener líneas tras el rechazo.");
    }

    @Test
    void modificarBolsaCerrada_LanzaExcepcion_NoCambiaEstado() {
        // Valida que no se pueda modificar una bolsa cerrada y que el estado no cambie.
        // Arrange
        BolsaDeEstilo bolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), UUID.randomUUID());
        bolsa.cerrarBolsa();
        LineaDeBolsa nuevaLinea = new LineaDeBolsa(UUID.randomUUID(), 1, new Dinero(new BigDecimal("50.00"), "USD"));

        // Act & Assert
        Exception exception = assertThrows(ModificacionBolsaNoPermitidaException.class, () ->
                bolsa.agregarLinea(nuevaLinea, 10)
        );
        assertEquals("Una bolsa de estilo cerrada o convertida en pedido no puede ser modificada.", exception.getMessage());
        assertTrue(bolsa.getLineas().isEmpty(), "La bolsa no debe contener líneas tras el rechazo.");
    }
}