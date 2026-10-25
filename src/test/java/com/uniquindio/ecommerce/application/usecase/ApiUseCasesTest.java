package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.exception.BolsaDeEstiloNoEncontradaException;
import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.InMemoryBolsaDeEstiloRepository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.InMemoryPrendaDeAutorRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ApiUseCasesTest {
    private PrendaDeAutor prenda(UUID creador) {
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), creador, "Camisa", "Lino",
                new Dinero(BigDecimal.TEN, "COP"),
                List.of(VariantePrenda.crear(UUID.randomUUID(), new SKU("CAM-M"), TipoTalla.M, 5)), 5);
    }

    @Test
    void listado_RecuperaTodasLasPrendasYFiltraPorCreador() {
        // Arrange
        var repo = new InMemoryPrendaDeAutorRepository();
        var registrar = new RegistrarPrendaDeAutorUseCase(repo);
        UUID creador = UUID.randomUUID();
        var primera = registrar.ejecutar(prenda(creador));
        var segunda = registrar.ejecutar(prenda(creador));
        registrar.ejecutar(prenda(UUID.randomUUID()));
        // Act
        var resultado = new ListarPrendasPorCreadorUseCase(repo).ejecutar(creador);
        // Assert
        assertEquals(2, resultado.size());
        assertTrue(resultado.containsAll(List.of(primera, segunda)));
        assertTrue(new ListarPrendasPorCreadorUseCase(repo).ejecutar(UUID.randomUUID()).isEmpty());
    }

    @Test
    void suspensionYStock_SeConservanEnElRepositorio() {
        // Arrange
        var repo = new InMemoryPrendaDeAutorRepository();
        var prenda = new RegistrarPrendaDeAutorUseCase(repo).ejecutar(prenda(UUID.randomUUID()));
        var varianteId = new VariantePrendaId(prenda.getVariantes().get(0).id());
        // Act
        new ActualizarStockVariantePrendaUseCase(repo).ejecutar(prenda.getId(), varianteId, 0);
        new SuspenderVentaPrendaDeAutorUseCase(repo).ejecutar(prenda.getId());
        // Assert
        var guardada = repo.obtenerPrendaPorId(prenda.getId()).orElseThrow();
        assertFalse(guardada.isDisponibleParaLaVenta());
        assertEquals(0, guardada.getVariantes().get(0).stockDisponible());
    }

    @Test
    void prendasYVariantesAusentes_SeDiferencianDeOtrasReglas() {
        // Arrange
        var repo = new InMemoryPrendaDeAutorRepository();
        var prenda = repo.guardarPrenda(prenda(UUID.randomUUID()));
        var stock = new ActualizarStockVariantePrendaUseCase(repo);
        var variante = new VariantePrendaId(UUID.randomUUID());
        // Act & Assert
        assertThrows(RecursoNoEncontradoException.class, () -> new SuspenderVentaPrendaDeAutorUseCase(repo).ejecutar(PrendaDeAutorId.random()));
        assertThrows(RecursoNoEncontradoException.class, () -> stock.ejecutar(PrendaDeAutorId.random(), variante, 1));
        assertThrows(RecursoNoEncontradoException.class, () -> stock.ejecutar(prenda.getId(), variante, 1));
    }

    @Test
    void bolsa_FlujoActivoVaciadoYCierreYErroresConservanReglas() {
        // Arrange
        var repo = new InMemoryBolsaDeEstiloRepository();
        UUID cliente = UUID.randomUUID();
        var bolsa = new CrearBolsaDeEstiloUseCase(repo).ejecutar(cliente);
        var vaciar = new VaciarBolsaDeEstiloUseCase(repo);
        var cerrar = new CerrarBolsaDeEstiloUseCase(repo);
        bolsa.agregarLinea(LineaDeBolsa.crear(UUID.randomUUID(), 1, new Dinero(BigDecimal.TEN, "COP")), 1);
        // Act
        vaciar.ejecutar(bolsa.getId());
        assertTrue(repo.obtenerBolsaPorId(bolsa.getId()).orElseThrow().getLineas().isEmpty());
        cerrar.ejecutar(bolsa.getId());
        // Assert
        assertTrue(new ObtenerBolsaDeEstiloActivaUseCase(repo).ejecutar(cliente).isEmpty());
        assertTrue(repo.obtenerBolsaPorId(bolsa.getId()).orElseThrow().isCerrada());
        assertThrows(ModificacionBolsaNoPermitidaException.class, () -> vaciar.ejecutar(bolsa.getId()));
        var ausente = new BolsaDeEstiloId(UUID.randomUUID());
        assertThrows(BolsaDeEstiloNoEncontradaException.class, () -> vaciar.ejecutar(ausente));
        assertThrows(BolsaDeEstiloNoEncontradaException.class, () -> cerrar.ejecutar(ausente));
    }
}
