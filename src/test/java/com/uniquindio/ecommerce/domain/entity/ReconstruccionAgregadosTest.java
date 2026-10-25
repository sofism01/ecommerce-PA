package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReconstruccionAgregadosTest {
    private Dinero precio() { return new Dinero(new BigDecimal("123.4500"), "COP"); }
    private LineaDeBolsa linea() { return LineaDeBolsa.crear(UUID.randomUUID(), 2, precio()); }

    @Test
    void prenda_ReconstruyeIdentidadEstadoYObjetosDeValor() {
        // Arrange
        var id = PrendaDeAutorId.random();
        var creador = UUID.randomUUID();
        var variante = VariantePrenda.crear(UUID.randomUUID(), new SKU("CAM-M"), TipoTalla.M, 0);
        // Act
        var prenda = PrendaDeAutor.reconstruir(id, creador, "Camisa", "Lino", precio(), List.of(variante), false, 0);
        // Assert
        assertEquals(id, prenda.getId());
        assertEquals(creador, prenda.getCreadorId());
        assertFalse(prenda.isDisponibleParaLaVenta());
        assertEquals(precio(), prenda.getPrecioBase());
        assertEquals(variante, prenda.getVariantes().get(0));
        assertThrows(ReglaDominioException.class, prenda::suspenderVenta);
    }

    @Test
    void prenda_ReconstruccionNoOmiteInvariantes() {
        // Arrange
        var id = PrendaDeAutorId.random();
        var creador = UUID.randomUUID();
        var variantes = List.of(VariantePrenda.crear(UUID.randomUUID(), new SKU("CAM-M"), TipoTalla.M, 1));
        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.reconstruir(null, creador, "Camisa", "", precio(), variantes, false, 1)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.reconstruir(id, creador, "Camisa", "", new Dinero(BigDecimal.ZERO, "COP"), variantes, false, 1)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.reconstruir(id, creador, "Camisa", "", precio(), List.of(), true, 0)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.reconstruir(id, creador, "Camisa", "", precio(), variantes, true, -1)));
    }

    @Test
    void bolsa_ReconstruyeCerradaConLineasSinSimularStock() {
        // Arrange
        var id = new BolsaDeEstiloId(UUID.randomUUID());
        var cliente = UUID.randomUUID();
        var linea = linea();
        var entrada = new ArrayList<>(List.of(linea));
        // Act
        var bolsa = BolsaDeEstilo.reconstruir(id, cliente, entrada, true);
        entrada.clear();
        // Assert
        assertEquals(id, bolsa.getId());
        assertEquals(cliente, bolsa.getClienteId());
        assertTrue(bolsa.isCerrada());
        assertEquals(2, bolsa.getLineas().get(0).cantidad());
        assertEquals(precio(), bolsa.getLineas().get(0).precioUnitario());
        assertThrows(ModificacionBolsaNoPermitidaException.class, bolsa::vaciar);
        assertThrows(ModificacionBolsaNoPermitidaException.class, () -> bolsa.agregarLinea(linea, 10));
    }

    @Test
    void bolsa_ReconstruccionRechazaNulosDuplicadosYMonedasMezcladas() {
        // Arrange
        var id = new BolsaDeEstiloId(UUID.randomUUID());
        var cliente = UUID.randomUUID();
        var linea = linea();
        var otraMoneda = LineaDeBolsa.crear(UUID.randomUUID(), 1, new Dinero(BigDecimal.ONE, "USD"));
        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.reconstruir(id, cliente, null, false)),
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.reconstruir(id, cliente, Arrays.asList(linea, null), false)),
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.reconstruir(id, cliente, List.of(linea, linea), false)),
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.reconstruir(id, cliente, List.of(linea, otraMoneda), true)));
    }
}
