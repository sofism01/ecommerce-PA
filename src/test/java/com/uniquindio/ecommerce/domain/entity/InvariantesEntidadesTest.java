package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.domain.exception.StockInsuficienteException;
import com.uniquindio.ecommerce.domain.service.ActualizadorDeStock;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvariantesEntidadesTest {

    private final UUID creadorId = UUID.randomUUID();

    private Dinero precio() {
        return new Dinero(new BigDecimal("50.00"), "COP");
    }

    private VariantePrenda variante(int stock) {
        return VariantePrenda.crear(UUID.randomUUID(), new SKU("SKU-1"), TipoTalla.M, stock);
    }

    private PrendaDeAutor prenda(List<VariantePrenda> variantes, int stock) {
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), creadorId, "Camisa", "Algodón", precio(), variantes, stock);
    }

    private VitrinaDeCreador vitrina() {
        return VitrinaDeCreador.crear(UUID.randomUUID(), creadorId, "BOHEME", "Ropa de autor", List.of());
    }

    private Creador creador(List<VitrinaDeCreador> vitrinas) {
        return Creador.crear(creadorId, "Ana", "ana@example.com", "123", "Taller", true, vitrinas);
    }

    private BolsaDeEstilo bolsa() {
        return BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), UUID.randomUUID());
    }

    private PedidoDeModa pedido() {
        return PedidoDeModa.crear(UUID.randomUUID(), UUID.randomUUID(), List.of(variante(2)));
    }

    @Test
    void entidades_SoloExponenConstructoresPrivadosYFabricaCrear() {
        // Arrange
        List<Class<?>> entidades = List.of(PrendaDeAutor.class, VariantePrenda.class,
                VarianteDeTalla.class, BolsaDeEstilo.class, PedidoDeModa.class, Creador.class, VitrinaDeCreador.class);

        // Act & Assert
        for (Class<?> entidad : entidades) {
            assertTrue(Arrays.stream(entidad.getDeclaredConstructors())
                    .allMatch(c -> Modifier.isPrivate(c.getModifiers())), entidad.getSimpleName());
            assertTrue(Arrays.stream(entidad.getDeclaredMethods()).anyMatch(m -> m.getName().equals("crear")
                    && Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())
                    && m.getReturnType().equals(entidad)), entidad.getSimpleName());
        }
        assertThrows(NoSuchMethodException.class, () -> PrendaDeAutor.class.getMethod("setStockTotal", int.class));
    }

    @Test
    void fabricas_RechazanIdentificadoresObligatoriosAusentes() {
        // Arrange
        VariantePrenda variante = variante(2);

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.crear(null, creadorId, "Camisa", "", precio(), List.of(variante), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.crear(PrendaDeAutorId.random(), null, "Camisa", "", precio(), List.of(variante), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> VariantePrenda.crear(null, new SKU("SKU"), TipoTalla.M, 0)),
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.crear(null, creadorId)),
                () -> assertThrows(ReglaDominioException.class, () -> BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), null)),
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(null, creadorId, List.of(variante))),
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(UUID.randomUUID(), null, List.of(variante))),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(null, "Ana", "a@b", "123", "Taller", true, List.of(vitrina()))),
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(null, creadorId, "Marca", "", List.of())),
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(UUID.randomUUID(), null, "Marca", "", List.of())));
    }

    @Test
    void prenda_RechazaNombrePrecioStockYVariantesInvalidos() {
        // Arrange
        VariantePrenda variante = variante(2);
        PrendaDeAutorId id = PrendaDeAutorId.random();

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.crear(id, creadorId, " ", "", precio(), List.of(variante), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.crear(id, creadorId, "Camisa", "", null, List.of(variante), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> PrendaDeAutor.crear(id, creadorId, "Camisa", "", new Dinero(BigDecimal.ZERO, "COP"), List.of(variante), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda(List.of(variante), -1)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda(null, 2)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda(List.of(), 0)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda(Arrays.asList(variante, null), 2)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda(List.of(variante, variante), 2)));
    }

    @Test
    void prenda_ActualizaStockValidoYRechazaNegativoSinMutar() {
        // Arrange
        PrendaDeAutor prenda = prenda(List.of(variante(2)), 2);

        // Act
        prenda.actualizarStock(0);

        // Assert
        assertEquals(0, prenda.getStockTotal());
        assertThrows(ReglaDominioException.class, () -> prenda.actualizarStock(-1));
        assertEquals(0, prenda.getStockTotal());
    }

    @Test
    void servicioDeStock_UtilizaOperacionValidadaDelDominio() {
        // Arrange
        PrendaDeAutor prenda = prenda(List.of(variante(2)), 2);
        PrendaDeAutorRepository repositorio = new PrendaDeAutorRepository() {
            public Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id) { return Optional.of(prenda); }
            public List<PrendaDeAutor> obtenerPrendasPorCreador(UUID id) { return List.of(prenda); }
            public PrendaDeAutor guardarPrenda(PrendaDeAutor valor) { return valor; }
        };
        ActualizadorDeStock servicio = new ActualizadorDeStock(repositorio);

        // Act & Assert
        assertThrows(ReglaDominioException.class, () -> servicio.actualizarStock(prenda.getId(), -1));
        assertEquals(2, prenda.getStockTotal());
    }

    @Test
    void prenda_ActualizaVarianteCorrectaConservandoIdentidadYOriginalInmutable() {
        // Arrange
        VariantePrenda variante = variante(2);
        VariantePrenda otra = variante(4);
        PrendaDeAutor prenda = prenda(List.of(variante, otra), 6);

        // Act
        prenda.actualizarStockVariante(new VariantePrendaId(variante.id()), 0);

        // Assert
        assertEquals(variante.id(), prenda.getVariantes().get(0).id());
        assertEquals(0, prenda.getVariantes().get(0).stockDisponible());
        assertEquals(4, prenda.getVariantes().get(1).stockDisponible());
        assertEquals(2, variante.stockDisponible());
        assertThrows(UnsupportedOperationException.class, () -> prenda.getVariantes().clear());
    }

    @Test
    void prenda_RechazaActualizacionesDeVarianteInvalidasSinMutar() {
        // Arrange
        VariantePrenda variante = variante(2);
        PrendaDeAutor prenda = prenda(List.of(variante), 2);

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> prenda.actualizarStockVariante(null, 1)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda.actualizarStockVariante(new VariantePrendaId(UUID.randomUUID()), 1)),
                () -> assertThrows(ReglaDominioException.class, () -> prenda.actualizarStockVariante(new VariantePrendaId(variante.id()), -1)));
        assertEquals(2, prenda.getVariantes().get(0).stockDisponible());
    }

    @Test
    void prenda_PrecioYSuspensionValidanYConservanIdentidad() {
        // Arrange
        PrendaDeAutor original = prenda(List.of(variante(2)), 2);
        Dinero nuevoPrecio = new Dinero(new BigDecimal("75"), "COP");

        // Act
        PrendaDeAutor actualizada = original.actualizarPrecio(nuevoPrecio);
        PrendaDeAutor suspendida = actualizada.suspenderVenta();

        // Assert
        assertEquals(original.getId(), suspendida.getId());
        assertEquals(nuevoPrecio, suspendida.getPrecioBase());
        assertFalse(suspendida.isDisponibleParaLaVenta());
        assertTrue(original.isDisponibleParaLaVenta());
        assertThrows(ReglaDominioException.class, suspendida::suspenderVenta);
        assertThrows(ReglaDominioException.class, () -> original.actualizarPrecio(null));
        assertThrows(ReglaDominioException.class, () -> original.actualizarPrecio(new Dinero(BigDecimal.ZERO, "COP")));
        assertEquals(precio(), original.getPrecioBase());
    }

    @Test
    void variantePrenda_ValidaCamposYOperaSinStockNegativo() {
        // Arrange
        VariantePrenda original = variante(2);

        // Act
        VariantePrenda reducida = original.reducirStock(2);
        VariantePrenda actualizada = original.actualizarStock(0);

        // Assert
        assertEquals(0, reducida.stockDisponible());
        assertEquals(original.id(), reducida.id());
        assertEquals(0, actualizada.stockDisponible());
        assertEquals(2, original.stockDisponible());
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> VariantePrenda.crear(UUID.randomUUID(), null, TipoTalla.M, 0)),
                () -> assertThrows(ReglaDominioException.class, () -> VariantePrenda.crear(UUID.randomUUID(), new SKU("SKU"), null, 0)),
                () -> assertThrows(ReglaDominioException.class, () -> variante(-1)),
                () -> assertThrows(ReglaDominioException.class, () -> original.actualizarStock(-1)),
                () -> assertThrows(ReglaDominioException.class, () -> original.reducirStock(0)),
                () -> assertThrows(ReglaDominioException.class, () -> original.reducirStock(-1)),
                () -> assertThrows(StockInsuficienteException.class, () -> original.reducirStock(3)));
    }

    @Test
    void varianteDeTalla_ValidaCamposYConservaStockAnteRechazo() {
        // Arrange
        SKU sku = new SKU("SKU");
        VarianteDeTalla variante = VarianteDeTalla.crear(sku, " m ", " Negro ", 2, precio());

        // Act
        boolean rechazada = variante.reducirStock(3);

        // Assert
        assertFalse(rechazada);
        assertEquals(2, variante.getStock());
        assertEquals("M", variante.getTalla());
        assertEquals("Negro", variante.getColor());
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> VarianteDeTalla.crear(null, "M", "Negro", 0, precio())),
                () -> assertThrows(ReglaDominioException.class, () -> VarianteDeTalla.crear(sku, " ", "Negro", 0, precio())),
                () -> assertThrows(ReglaDominioException.class, () -> VarianteDeTalla.crear(sku, "M", null, 0, precio())),
                () -> assertThrows(ReglaDominioException.class, () -> VarianteDeTalla.crear(sku, "M", "Negro", -1, precio())),
                () -> assertThrows(ReglaDominioException.class, () -> VarianteDeTalla.crear(sku, "M", "Negro", 0, null)),
                () -> assertThrows(ReglaDominioException.class, () -> variante.reducirStock(0)),
                () -> assertThrows(ReglaDominioException.class, () -> variante.reducirStock(-1)));
        assertTrue(variante.reducirStock(2));
        assertEquals(0, variante.getStock());
    }

    @Test
    void bolsa_AcumulaCantidadYRechazaExcesoSinMutar() {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        UUID varianteId = UUID.randomUUID();

        // Act
        bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, 2, precio()), 4);
        bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, 2, precio()), 4);

        // Assert
        assertEquals(1, bolsa.getLineas().size());
        assertEquals(4, bolsa.getLineas().get(0).cantidad());
        assertThrows(StockInsuficienteException.class, () -> bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, 1, precio()), 4));
        assertEquals(4, bolsa.getLineas().get(0).cantidad());
        assertThrows(UnsupportedOperationException.class, () -> bolsa.getLineas().clear());
    }

    @Test
    void bolsa_RechazaEntradasInvalidasYMezclaDeMonedasSinMutar() {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        LineaDeBolsa linea = LineaDeBolsa.crear(UUID.randomUUID(), 1, precio());
        bolsa.agregarLinea(linea, 2);
        LineaDeBolsa otraMoneda = LineaDeBolsa.crear(UUID.randomUUID(), 1, new Dinero(BigDecimal.ONE, "USD"));
        LineaDeBolsa mismaVarianteOtraMoneda = LineaDeBolsa.crear(linea.varianteId(), 1, otraMoneda.precioUnitario());

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> bolsa.agregarLinea(null, 2)),
                () -> assertThrows(ReglaDominioException.class, () -> bolsa.agregarLinea(linea, -1)),
                () -> assertThrows(ReglaDominioException.class, () -> bolsa.agregarLinea(otraMoneda, 2)),
                () -> assertThrows(ReglaDominioException.class, () -> bolsa.agregarLinea(mismaVarianteOtraMoneda, 2)));
        assertEquals(1, bolsa.getLineas().size());
        assertEquals(1, bolsa.getLineas().get(0).cantidad());
    }

    @Test
    void bolsa_CantidadAcumuladaNoDesbordaEntero() {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        UUID varianteId = UUID.randomUUID();
        bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, Integer.MAX_VALUE, precio()), Integer.MAX_VALUE);

        // Act & Assert
        assertThrows(StockInsuficienteException.class, () -> bolsa.agregarLinea(LineaDeBolsa.crear(varianteId, 1, precio()), Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, bolsa.getLineas().get(0).cantidad());
    }

    @Test
    void bolsa_CerradaImpideAgregarYVaciarPeroActivaPermiteVaciar() {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        LineaDeBolsa linea = LineaDeBolsa.crear(UUID.randomUUID(), 1, precio());
        bolsa.agregarLinea(linea, 1);
        bolsa.vaciar();
        assertTrue(bolsa.getLineas().isEmpty());
        bolsa.agregarLinea(linea, 1);

        // Act
        bolsa.cerrarBolsa();

        // Assert
        assertFalse(bolsa.estaActiva());
        assertThrows(ModificacionBolsaNoPermitidaException.class, () -> bolsa.agregarLinea(linea, 2));
        assertThrows(ModificacionBolsaNoPermitidaException.class, bolsa::vaciar);
        assertEquals(1, bolsa.getLineas().size());
    }

    @Test
    void creador_ValidaDatosObligatoriosYListas() {
        // Arrange
        List<VitrinaDeCreador> vitrinas = List.of(vitrina());

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, " ", "a@b", "123", "Taller", true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, "Ana", "@", "123", "Taller", true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, "Ana", "a b@c", "123", "Taller", true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, "Ana", null, "123", "Taller", true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, "Ana", "a@b", " ", "Taller", true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> Creador.crear(creadorId, "Ana", "a@b", "123", null, true, vitrinas)),
                () -> assertThrows(ReglaDominioException.class, () -> creador(null)),
                () -> assertThrows(ReglaDominioException.class, () -> creador(List.of())),
                () -> assertThrows(ReglaDominioException.class, () -> creador(Arrays.asList(vitrina(), null))));
    }

    @Test
    void creador_EncapsulaVitrinasYConservaAlMenosUnaAlEliminar() {
        // Arrange
        VitrinaDeCreador primera = vitrina();
        VitrinaDeCreador segunda = vitrina();
        List<VitrinaDeCreador> entrada = new ArrayList<>(List.of(primera));
        Creador creador = creador(entrada);
        entrada.clear();

        // Act
        creador.agregarVitrina(segunda);
        creador.eliminarVitrina(primera.getId());

        // Assert
        assertEquals(List.of(segunda), creador.getVitrinas());
        assertThrows(ReglaDominioException.class, () -> creador.eliminarVitrina(segunda.getId()));
        assertThrows(ReglaDominioException.class, () -> creador.eliminarVitrina(null));
        assertThrows(ReglaDominioException.class, () -> creador.agregarVitrina(null));
        assertThrows(UnsupportedOperationException.class, () -> creador.getVitrinas().clear());
        assertEquals(1, creador.getVitrinas().size());
    }

    @Test
    void vitrina_EncapsulaCatalogoYPermiteOperacionesValidas() {
        // Arrange
        PrendaDeAutor prenda = prenda(List.of(variante(2)), 2);
        List<PrendaDeAutor> entrada = new ArrayList<>(List.of(prenda));
        VitrinaDeCreador vitrina = VitrinaDeCreador.crear(UUID.randomUUID(), creadorId, "Marca", "", entrada);
        entrada.clear();
        assertEquals(1, vitrina.getCatalogo().size());

        // Act
        vitrina.eliminarPrenda(prenda.getId().value());
        assertTrue(vitrina.getCatalogo().isEmpty());
        vitrina.agregarPrenda(prenda);

        // Assert
        assertEquals(List.of(prenda), vitrina.getCatalogo());
        assertThrows(UnsupportedOperationException.class, () -> vitrina.getCatalogo().clear());
        assertThrows(ReglaDominioException.class, () -> vitrina.agregarPrenda(null));
        assertThrows(ReglaDominioException.class, () -> vitrina.eliminarPrenda(null));
    }

    @Test
    void vitrina_RechazaDatosYCatalogoInvalidos() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(id, creadorId, " ", "", List.of())),
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(id, creadorId, "Marca", null, List.of())),
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(id, creadorId, "Marca", "", null)),
                () -> assertThrows(ReglaDominioException.class, () -> VitrinaDeCreador.crear(id, creadorId, "Marca", "", Arrays.asList((PrendaDeAutor) null))));
    }

    @Test
    void pedido_RechazaVariantesInvalidasYProtegeLista() {
        // Arrange
        UUID id = UUID.randomUUID();
        VariantePrenda variante = variante(2);
        List<VariantePrenda> entrada = new ArrayList<>(List.of(variante));
        PedidoDeModa pedido = PedidoDeModa.crear(id, creadorId, entrada);

        // Act
        entrada.clear();

        // Assert
        assertEquals(List.of(variante), pedido.getVariantes());
        assertThrows(UnsupportedOperationException.class, () -> pedido.getVariantes().clear());
        assertAll(
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(id, creadorId, null)),
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(id, creadorId, List.of())),
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(id, creadorId, Arrays.asList((VariantePrenda) null))),
                () -> assertThrows(ReglaDominioException.class, () -> PedidoDeModa.crear(id, creadorId, List.of(variante(0)))));
    }

    @Test
    void pedido_RespetaSecuenciaYRechazaSaltosSinCambiarEstado() {
        // Arrange
        PedidoDeModa pedido = pedido();

        // Act & Assert
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertThrows(ReglaDominioException.class, pedido::despachar);
        assertThrows(ReglaDominioException.class, pedido::completar);
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        pedido.autorizarPago();
        assertEquals(EstadoPedido.PAGADO, pedido.getEstado());
        assertThrows(ReglaDominioException.class, pedido::autorizarPago);
        assertThrows(ReglaDominioException.class, pedido::completar);
        assertEquals(EstadoPedido.PAGADO, pedido.getEstado());
        pedido.despachar();
        assertEquals(EstadoPedido.DESPACHADO, pedido.getEstado());
        assertThrows(ReglaDominioException.class, pedido::despachar);
        pedido.completar();
        assertEquals(EstadoPedido.COMPLETADO, pedido.getEstado());
        assertThrows(ReglaDominioException.class, pedido::cancelar);
        assertThrows(ReglaDominioException.class, pedido::autorizarPago);
        assertThrows(ReglaDominioException.class, pedido::despachar);
        assertThrows(ReglaDominioException.class, pedido::completar);
        assertEquals(EstadoPedido.COMPLETADO, pedido.getEstado());
    }

    @Test
    void pedido_CanceladoNoPuedeContinuarProceso() {
        // Arrange
        PedidoDeModa pedido = pedido();

        // Act
        pedido.cancelar();

        // Assert
        assertThrows(ReglaDominioException.class, pedido::autorizarPago);
        assertThrows(ReglaDominioException.class, pedido::despachar);
        assertThrows(ReglaDominioException.class, pedido::completar);
        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
    }
}
