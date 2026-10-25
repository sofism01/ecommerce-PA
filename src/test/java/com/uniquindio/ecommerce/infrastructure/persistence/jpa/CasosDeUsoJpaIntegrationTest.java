package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.seed.DatosIniciales;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:boheme_jpa_integracion", "spring.jpa.show-sql=false"})
@Transactional
class CasosDeUsoJpaIntegrationTest {
    @Autowired PrendaDeAutorRepository prendas;
    @Autowired BolsaDeEstiloRepository bolsas;
    @Autowired RegistrarPrendaDeAutorUseCase registrar;
    @Autowired ListarPrendasPorCreadorUseCase listar;
    @Autowired SuspenderVentaPrendaDeAutorUseCase suspender;
    @Autowired ActualizarStockVariantePrendaUseCase actualizarStock;
    @Autowired CrearBolsaDeEstiloUseCase crearBolsa;
    @Autowired ObtenerBolsaDeEstiloActivaUseCase obtenerActiva;
    @Autowired VaciarBolsaDeEstiloUseCase vaciar;
    @Autowired CerrarBolsaDeEstiloUseCase cerrar;
    @Autowired DatosIniciales iniciales;
    @Autowired ApplicationContext context;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private PrendaDeAutor prenda() {
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), UUID.randomUUID(), "Camisa integración", "Lino",
                new Dinero(new BigDecimal("20.00"), "COP"), List.of(
                        VariantePrenda.crear(UUID.randomUUID(), new SKU("INT-M"), TipoTalla.M, 5)), 5);
    }

    private void recargar() { em.flush(); em.clear(); }

    @Test
    void aplicacion_UtilizaJpaYCargaDatosIdempotentesSinResucitarEliminados() {
        // Arrange
        assertInstanceOf(JpaPrendaDeAutorRepository.class, prendas);
        assertInstanceOf(JpaBolsaDeEstiloRepository.class, bolsas);
        assertTrue(context.getBeansOfType(InMemoryPrendaDeAutorRepository.class).isEmpty());
        assertTrue(context.getBeansOfType(InMemoryBolsaDeEstiloRepository.class).isEmpty());
        var id = new PrendaDeAutorId(UUID.fromString("91000000-0000-4000-8000-000000000001"));
        var modificada = prendas.obtenerPrendaPorId(id).orElseThrow()
                .actualizarPrecio(new Dinero(new BigDecimal("42.75"), "COP"));
        prendas.guardarPrenda(modificada);
        recargar();
        // Act
        iniciales.run();
        iniciales.run();
        recargar();
        // Assert
        assertEquals(3, jdbc.queryForObject("select count(*) from prendas_autor", Integer.class));
        assertEquals(5, jdbc.queryForObject("select count(*) from variantes_prenda", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from bolsas_estilo", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from lineas_bolsa", Integer.class));
        assertEquals(modificada.getPrecioBase(), prendas.obtenerPrendaPorId(id).orElseThrow().getPrecioBase());
        ((JpaPrendaDeAutorRepository) prendas).eliminarLogicamente(id);
        recargar();
        iniciales.run();
        recargar();
        assertTrue(prendas.obtenerPrendaPorId(id).isEmpty());
        assertEquals(3, jdbc.queryForObject("select count(*) from prendas_autor", Integer.class));
    }

    @Test
    void cuatroCasosDeUsoDePrendas_EscribenYLeenEnH2() {
        // Arrange
        var original = registrar.ejecutar(prenda());
        recargar();
        var variante = original.getVariantes().get(0).id();
        // Act
        actualizarStock.ejecutar(original.getId(), new VariantePrendaId(variante), 0);
        suspender.ejecutar(original.getId());
        recargar();
        var listado = listar.ejecutar(original.getCreadorId());
        // Assert
        assertEquals(1, listado.size());
        assertFalse(listado.get(0).isDisponibleParaLaVenta());
        assertEquals(0, listado.get(0).getVariantes().get(0).stockDisponible());
        assertEquals(0, jdbc.queryForObject("select stock_disponible from variantes_prenda where id = ?", Integer.class, variante));
        assertFalse(jdbc.queryForObject("select eliminado from prendas_autor where id = ?", Boolean.class, original.getId().value()));
    }

    @Test
    void cuatroCasosDeUsoDeBolsas_PersistenContenidoVaciadoYCierre() {
        // Arrange
        var bolsa = crearBolsa.ejecutar(UUID.randomUUID());
        var seleccion = obtenerActiva.ejecutar(bolsa.getClienteId()).orElseThrow();
        seleccion.agregarLinea(LineaDeBolsa.crear(UUID.randomUUID(), 1, new Dinero(new BigDecimal("15.00"), "COP")), 1);
        bolsas.guardarBolsa(seleccion);
        recargar();
        assertEquals(1, obtenerActiva.ejecutar(bolsa.getClienteId()).orElseThrow().getLineas().size());
        // Act
        vaciar.ejecutar(bolsa.getId());
        cerrar.ejecutar(bolsa.getId());
        recargar();
        // Assert
        assertTrue(obtenerActiva.ejecutar(bolsa.getClienteId()).isEmpty());
        assertTrue(bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow().isCerrada());
        assertTrue(jdbc.queryForObject("select cerrada from bolsas_estilo where id = ?", Boolean.class, bolsa.getId().valor()));
        assertFalse(jdbc.queryForObject("select eliminado from bolsas_estilo where id = ?", Boolean.class, bolsa.getId().valor()));
        assertEquals(1, jdbc.queryForObject("select count(*) from lineas_bolsa where bolsa_id = ? and eliminado = true", Integer.class, bolsa.getId().valor()));
        assertThrows(ModificacionBolsaNoPermitidaException.class, () -> vaciar.ejecutar(bolsa.getId()));
    }

    @Test
    void modificarObjetoDeDominioSinGuardar_NoModificaLaBaseDeDatos() {
        // Arrange
        var original = prenda();
        registrar.ejecutar(original);
        recargar();
        // Act
        original.actualizarStock(0);
        recargar();
        // Assert
        assertEquals(5, prendas.obtenerPrendaPorId(original.getId()).orElseThrow().getStockTotal());
        assertEquals(5, jdbc.queryForObject("select stock_total from prendas_autor where id = ?", Integer.class, original.getId().value()));
    }
}
