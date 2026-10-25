package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({JpaPrendaDeAutorRepository.class, JpaBolsaDeEstiloRepository.class,
        PrendaJpaMapper.class, BolsaJpaMapper.class, DineroJpaMapper.class})
class PersistenciaJpaTest {
    @Autowired JpaPrendaDeAutorRepository prendas;
    @Autowired JpaBolsaDeEstiloRepository bolsas;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    private Dinero dinero() { return new Dinero(new BigDecimal("150000.1234500"), "COP"); }

    private PrendaDeAutor prenda(UUID creador) {
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), creador, "Camisa de lino", "Hecha a mano", dinero(), List.of(
                VariantePrenda.crear(UUID.randomUUID(), new SKU("CAM-M"), TipoTalla.M, 5),
                VariantePrenda.crear(UUID.randomUUID(), new SKU("CAM-L"), TipoTalla.L, 3)), 8);
    }

    private BolsaDeEstilo bolsa() {
        var bolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), UUID.randomUUID());
        bolsa.agregarLinea(LineaDeBolsa.crear(UUID.randomUUID(), 2, dinero()), 5);
        bolsa.agregarLinea(LineaDeBolsa.crear(UUID.randomUUID(), 1, new Dinero(new BigDecimal("70.00"), "COP")), 3);
        return bolsa;
    }

    private void recargar() {
        entityManager.flush();
        entityManager.clear();
    }

    private long contar(String tabla, String columna, UUID id, String condicion) {
        return jdbc.queryForObject("select count(*) from " + tabla + " where " + columna + " = ?" + condicion, Long.class, id);
    }

    @Test
    void guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado() {
        // Arrange
        var original = prenda(UUID.randomUUID()).suspenderVenta();
        // Act
        prendas.guardarPrenda(original);
        recargar();
        var leida = prendas.obtenerPrendaPorId(original.getId()).orElseThrow();
        // Assert
        assertEquals(original.getId(), leida.getId());
        assertEquals(original.getCreadorId(), leida.getCreadorId());
        assertEquals(original.getNombre(), leida.getNombre());
        assertEquals(original.getDescripcion(), leida.getDescripcion());
        assertEquals(original.getPrecioBase(), leida.getPrecioBase());
        assertEquals(8, leida.getStockTotal());
        assertFalse(leida.isDisponibleParaLaVenta());
        assertEquals(2, leida.getVariantes().size());
        for (int i = 0; i < 2; i++) {
            var esperada = original.getVariantes().get(i);
            var actual = leida.getVariantes().get(i);
            assertEquals(esperada.id(), actual.id());
            assertEquals(esperada.sku(), actual.sku());
            assertEquals(esperada.tipoTalla(), actual.tipoTalla());
            assertEquals(esperada.stockDisponible(), actual.stockDisponible());
        }
        assertEquals(2, contar("variantes_prenda", "prenda_id", original.getId().value(), ""));
    }

    @Test
    void actualizarPrenda_PersistePrecioStocksYSuspensionSinDuplicarVariantes() {
        // Arrange
        var original = prendas.guardarPrenda(prenda(UUID.randomUUID()));
        recargar();
        var editada = prendas.obtenerPrendaPorId(original.getId()).orElseThrow();
        editada.actualizarStockVariante(new VariantePrendaId(editada.getVariantes().get(0).id()), 0);
        editada.actualizarStock(3);
        editada = editada.actualizarPrecio(new Dinero(new BigDecimal("234.5678900"), "USD")).suspenderVenta();
        // Act
        prendas.guardarPrenda(editada);
        recargar();
        var leida = prendas.obtenerPrendaPorId(original.getId()).orElseThrow();
        // Assert
        assertEquals(editada.getPrecioBase(), leida.getPrecioBase());
        assertEquals(0, leida.getVariantes().get(0).stockDisponible());
        assertEquals(3, leida.getStockTotal());
        assertFalse(leida.isDisponibleParaLaVenta());
        assertEquals(2, contar("variantes_prenda", "prenda_id", original.getId().value(), ""));
        assertEquals(0, contar("variantes_prenda", "prenda_id", original.getId().value(), " and eliminado = true"));
    }

    @Test
    void guardarYRecuperarBolsaCerrada_ConservaLineasPreciosOrdenYBloqueo() {
        // Arrange
        var original = bolsa();
        original.cerrarBolsa();
        // Act
        bolsas.guardarBolsa(original);
        recargar();
        var leida = bolsas.obtenerBolsaPorId(original.getId()).orElseThrow();
        // Assert
        assertEquals(original.getId(), leida.getId());
        assertEquals(original.getClienteId(), leida.getClienteId());
        assertTrue(leida.isCerrada());
        assertEquals(2, leida.getLineas().size());
        for (int i = 0; i < 2; i++) {
            var esperada = original.getLineas().get(i);
            var actual = leida.getLineas().get(i);
            assertEquals(esperada.varianteId(), actual.varianteId());
            assertEquals(esperada.cantidad(), actual.cantidad());
            assertEquals(esperada.precioUnitario(), actual.precioUnitario());
            assertEquals(esperada.calcularSubtotal(), actual.calcularSubtotal());
        }
        assertThrows(ModificacionBolsaNoPermitidaException.class, leida::vaciar);
        assertTrue(bolsas.obtenerBolsaActivaPorCliente(original.getClienteId()).isEmpty());
    }

    @Test
    void actualizarCantidad_ConservaIdentidadTecnicaDeLaLinea() {
        // Arrange
        var original = bolsas.guardarBolsa(bolsa());
        recargar();
        var primera = original.getLineas().get(0);
        UUID idFila = jdbc.queryForObject("select id from lineas_bolsa where bolsa_id = ? and variante_id = ?", UUID.class,
                original.getId().valor(), primera.varianteId());
        var editada = bolsas.obtenerBolsaPorId(original.getId()).orElseThrow();
        editada.agregarLinea(LineaDeBolsa.crear(primera.varianteId(), 1, primera.precioUnitario()), 5);
        // Act
        bolsas.guardarBolsa(editada);
        recargar();
        // Assert
        assertEquals(3, bolsas.obtenerBolsaPorId(original.getId()).orElseThrow().getLineas().get(0).cantidad());
        assertEquals(3, jdbc.queryForObject("select cantidad from lineas_bolsa where id = ?", Integer.class, idFila));
        assertEquals(2, contar("lineas_bolsa", "bolsa_id", original.getId().valor(), ""));
    }

    @Test
    void eliminarPrenda_OcultaAgregadoYMarcaVariantesSinBorrarFilas() {
        // Arrange
        var original = prendas.guardarPrenda(prenda(UUID.randomUUID()));
        recargar();
        // Act
        prendas.eliminarLogicamente(original.getId());
        recargar();
        // Assert
        assertTrue(prendas.obtenerPrendaPorId(original.getId()).isEmpty());
        assertTrue(prendas.obtenerPrendasPorCreador(original.getCreadorId()).isEmpty());
        assertEquals(1, contar("prendas_autor", "id", original.getId().value(), " and eliminado = true"));
        assertEquals(2, contar("variantes_prenda", "prenda_id", original.getId().value(), " and eliminado = true"));
    }

    @Test
    void eliminarBolsa_OcultaAgregadoYMarcaLineasSinBorrarFilas() {
        // Arrange
        var original = bolsas.guardarBolsa(bolsa());
        recargar();
        // Act
        bolsas.eliminarLogicamente(original.getId());
        recargar();
        // Assert
        assertTrue(bolsas.obtenerBolsaPorId(original.getId()).isEmpty());
        assertTrue(bolsas.obtenerBolsaActivaPorCliente(original.getClienteId()).isEmpty());
        assertEquals(1, contar("bolsas_estilo", "id", original.getId().valor(), " and eliminado = true"));
        assertEquals(2, contar("lineas_bolsa", "bolsa_id", original.getId().valor(), " and eliminado = true"));
    }

    @Test
    void vaciarYAgregarDeNuevo_ConservaHistorialDeLineasSinDuplicadosActivos() {
        // Arrange
        var original = bolsas.guardarBolsa(bolsa());
        var linea = original.getLineas().get(0);
        recargar();
        var editada = bolsas.obtenerBolsaPorId(original.getId()).orElseThrow();
        editada.vaciar();
        // Act
        bolsas.guardarBolsa(editada);
        recargar();
        var vacia = bolsas.obtenerBolsaPorId(original.getId()).orElseThrow();
        // Assert
        assertTrue(vacia.getLineas().isEmpty());
        assertEquals(2, contar("lineas_bolsa", "bolsa_id", original.getId().valor(), " and eliminado = true"));
        vacia.agregarLinea(linea, 5);
        bolsas.guardarBolsa(vacia);
        recargar();
        assertEquals(1, bolsas.obtenerBolsaPorId(original.getId()).orElseThrow().getLineas().size());
        assertEquals(3, contar("lineas_bolsa", "bolsa_id", original.getId().valor(), ""));
        assertEquals(1, contar("lineas_bolsa", "bolsa_id", original.getId().valor(), " and eliminado = false"));
    }

    @Test
    void retirarVarianteDelAgregado_UsaEliminacionLogicaDelHuerfano() {
        // Arrange
        var original = prendas.guardarPrenda(prenda(UUID.randomUUID()));
        recargar();
        var editada = PrendaDeAutor.reconstruir(original.getId(), original.getCreadorId(), original.getNombre(),
                original.getDescripcion(), original.getPrecioBase(), List.of(original.getVariantes().get(1)), true, 3);
        // Act
        prendas.guardarPrenda(editada);
        recargar();
        // Assert
        assertEquals(1, prendas.obtenerPrendaPorId(original.getId()).orElseThrow().getVariantes().size());
        assertEquals(2, contar("variantes_prenda", "prenda_id", original.getId().value(), ""));
        assertEquals(1, contar("variantes_prenda", "prenda_id", original.getId().value(), " and eliminado = true"));
    }

    @Test
    void guardarAgregadosEliminados_NoLosResucita() {
        // Arrange
        var prenda = prendas.guardarPrenda(prenda(UUID.randomUUID()));
        var bolsa = bolsas.guardarBolsa(bolsa());
        recargar();
        prendas.eliminarLogicamente(prenda.getId());
        bolsas.eliminarLogicamente(bolsa.getId());
        recargar();
        // Act & Assert
        assertThrows(RecursoNoEncontradoException.class, () -> prendas.guardarPrenda(prenda));
        assertThrows(RecursoNoEncontradoException.class, () -> bolsas.guardarBolsa(bolsa));
        assertEquals(1, contar("prendas_autor", "id", prenda.getId().value(), " and eliminado = true"));
        assertEquals(1, contar("bolsas_estilo", "id", bolsa.getId().valor(), " and eliminado = true"));
    }

    @Test
    void listado_FiltraPorCreadorYExcluyeEliminados() {
        // Arrange
        UUID creador = UUID.randomUUID();
        var primera = prendas.guardarPrenda(prenda(creador));
        var segunda = prendas.guardarPrenda(prenda(creador));
        var eliminada = prendas.guardarPrenda(prenda(creador));
        prendas.guardarPrenda(prenda(UUID.randomUUID()));
        recargar();
        prendas.eliminarLogicamente(eliminada.getId());
        recargar();
        // Act
        var resultado = prendas.obtenerPrendasPorCreador(creador);
        // Assert
        assertEquals(2, resultado.size());
        assertTrue(resultado.containsAll(List.of(primera, segunda)));
    }

    @Test
    void eliminarCatalogo_NoDestruyeSeleccionHistoricaDeUnaBolsa() {
        // Arrange
        var prenda = prendas.guardarPrenda(prenda(UUID.randomUUID()));
        var bolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), UUID.randomUUID());
        var varianteId = prenda.getVariantes().get(0).id();
        bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, 2, prenda.getPrecioBase()), 5);
        bolsas.guardarBolsa(bolsa);
        recargar();
        // Act
        prendas.eliminarLogicamente(prenda.getId());
        recargar();
        var historica = bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow();
        // Assert
        assertEquals(varianteId, historica.getLineas().get(0).varianteId());
        assertEquals(prenda.getPrecioBase(), historica.getLineas().get(0).precioUnitario());
        assertEquals(2, historica.getLineas().get(0).cantidad());
    }

    @Test
    void dinero_ConservaPrecisionAltaYEscalaOriginalSinRedondear() {
        // Arrange
        var original = prenda(UUID.randomUUID()).actualizarPrecio(new Dinero(
                new BigDecimal("1234567890123456789012345678901234567890.1234500"), "COP"));
        // Act
        prendas.guardarPrenda(original);
        recargar();
        var leida = prendas.obtenerPrendaPorId(original.getId()).orElseThrow();
        // Assert
        assertEquals(original.getPrecioBase(), leida.getPrecioBase());
        assertEquals(7, leida.getPrecioBase().monto().scale());
    }
}
