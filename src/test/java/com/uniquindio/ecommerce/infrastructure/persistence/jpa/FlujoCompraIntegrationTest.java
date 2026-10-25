package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.application.security.AccesoDenegadoException;
import com.uniquindio.ecommerce.domain.entity.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.domain.exception.*;
import com.uniquindio.ecommerce.domain.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:flujo_compra;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "boheme.datos-iniciales.enabled=false","spring.jpa.show-sql=false"})
class FlujoCompraIntegrationTest {
    @Autowired AgregarVarianteABolsaUseCase agregar;
    @Autowired ConfirmarPedidoUseCase confirmar;
    @Autowired ConsultarPedidoUseCase consultar;
    @Autowired PrendaDeAutorRepository prendas;
    @Autowired BolsaDeEstiloRepository bolsas;
    @Autowired PedidoDeModaRepository pedidos;
    @MockitoSpyBean JpaPedidoDeModaRepository pedidoJpa;
    @Autowired JdbcTemplate jdbc;
    UUID comprador;
    PrendaDeAutor prenda;
    BolsaDeEstilo bolsa;

    private void identidad(UUID id) {
        var jwt=Jwt.withTokenValue("identidad-sólo-prueba-directa").header("alg","HS256").subject(id.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
    private PrendaDeAutor prenda(String moneda) {
        return prendas.guardarPrenda(PrendaDeAutor.crear(PrendaDeAutorId.random(),UUID.randomUUID(),"Camisa","Lino",
                new Dinero(new BigDecimal("19.9500"),moneda),List.of(
                    VariantePrenda.crear(UUID.randomUUID(),new SKU("CAM-M"),TipoTalla.M,3)),3));
    }
    private UUID variante() { return prenda.getVariantes().getFirst().id(); }
    private int stock() { return prendas.obtenerPrendaPorId(prenda.getId()).orElseThrow().getVariantes().getFirst().stockDisponible(); }
    private long numeroPedidos() { return jdbc.queryForObject("select count(*) from pedidos_moda",Long.class); }
    @BeforeEach void preparar() {
        // Fixtures comprometidos: las pruebas de concurrencia usan conexiones y transacciones distintas.
        for(String tabla:List.of("lineas_pedido","pedidos_moda","lineas_bolsa","bolsas_estilo","variantes_prenda","prendas_autor"))
            jdbc.update("delete from "+tabla);
        comprador=UUID.randomUUID(); identidad(comprador);
        prenda=prenda("COP"); bolsa=bolsas.guardarBolsa(BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()),comprador));
    }
    @AfterEach void limpiarIdentidad() { SecurityContextHolder.clearContext(); }
    @Test void agregaVarianteDesdeCatalogoConPrecioYSubtotalCorrectosSinReservarStock() {
        // Arrange
        var variante=variante();
        // Act
        var actual=agregar.ejecutar(bolsa.getId().valor(),variante,2);
        // Assert
        assertEquals(2,actual.getLineas().getFirst().cantidad());
        assertEquals(prenda.getPrecioBase(),actual.getLineas().getFirst().precioUnitario());
        assertEquals(new BigDecimal("39.9000"),actual.getLineas().getFirst().calcularSubtotal().monto());
        assertEquals(3,stock()); assertEquals(0,numeroPedidos());
    }
    @Test void excesoDeStockYAcumulacionSeRechazanSinMutarInventarioOPedidos() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        // Act & Assert
        assertThrows(StockInsuficienteException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),4));
        assertThrows(StockInsuficienteException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),2));
        assertEquals(2,bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow().getLineas().getFirst().cantidad());
        assertEquals(3,stock()); assertEquals(0,numeroPedidos());
    }
    @Test void bolsaCerradaNoAceptaItemsNiConfirmacion() {
        // Arrange
        bolsa.cerrarBolsa(); bolsas.guardarBolsa(bolsa);
        // Act & Assert
        assertThrows(ModificacionBolsaNoPermitidaException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),1));
        assertThrows(ModificacionBolsaNoPermitidaException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
    }
    @Test void prendaSuspendidaNoSeAgregaNiSeConfirma() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),1);
        prendas.guardarPrenda(prenda.suspenderVenta());
        // Act & Assert
        assertThrows(ReglaDominioException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),1));
        assertThrows(ReglaDominioException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        assertEquals(0,numeroPedidos()); assertEquals(3,stock());
    }
    @Test void respetaMonedaYRechazaVarianteInexistenteYCantidadInvalida() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),1);
        var usd=prenda("USD");
        // Act & Assert
        assertThrows(ReglaDominioException.class,() -> agregar.ejecutar(bolsa.getId().valor(),usd.getVariantes().getFirst().id(),1));
        assertThrows(RecursoNoEncontradoException.class,() -> agregar.ejecutar(bolsa.getId().valor(),UUID.randomUUID(),1));
        assertThrows(ReglaDominioException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),0));
    }
    @Test void rechazaBolsaAjenaYConsultaDePedidoAjeno() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),1);
        var p=confirmar.ejecutar(bolsa.getId().valor()); identidad(UUID.randomUUID());
        // Act & Assert
        assertThrows(AccesoDenegadoException.class,() -> agregar.ejecutar(bolsa.getId().valor(),variante(),1));
        assertThrows(AccesoDenegadoException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        assertThrows(AccesoDenegadoException.class,() -> consultar.ejecutar(p.getId()));
    }
    @Test void pedidoPersistePendienteDescuentaVariantesCierraBolsaYSeRecupera() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        // Act
        var creado=confirmar.ejecutar(bolsa.getId().valor());
        var leido=consultar.ejecutar(creado.getId());
        // Assert
        assertEquals(EstadoPedido.PENDIENTE,leido.getEstado()); assertEquals(comprador,leido.getClienteId());
        assertEquals(creado.getFechaCreacion(),leido.getFechaCreacion());
        assertEquals(bolsa.getId().valor(),leido.getBolsaId()); assertEquals(1,stock());
        assertEquals(3,prendas.obtenerPrendaPorId(prenda.getId()).orElseThrow().getStockTotal());
        assertEquals(new Dinero(new BigDecimal("39.9000"),"COP"),leido.calcularTotal());
        assertTrue(bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow().isCerrada());
        assertEquals(1,numeroPedidos());
    }
    @Test void conservaPrecioHistoricoAunqueCambieCatalogoAntesYDespuesDeConfirmar() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        prendas.guardarPrenda(prenda.actualizarPrecio(new Dinero(new BigDecimal("80"),"COP")));
        // Act
        var p=confirmar.ejecutar(bolsa.getId().valor());
        ((JpaPrendaDeAutorRepository)prendas).eliminarLogicamente(prenda.getId());
        var leido=pedidos.porId(p.getId()).orElseThrow();
        // Assert
        assertEquals(new BigDecimal("19.9500"),leido.getLineas().getFirst().precioUnitario().monto());
        assertEquals(new BigDecimal("39.9000"),leido.calcularTotal().monto());
        assertEquals("Camisa",leido.getLineas().getFirst().nombrePrenda());
    }
    @Test void noConfirmaDosVecesNiPermiteBolsaVacia() {
        // Arrange & Act & Assert
        assertThrows(ReglaDominioException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        confirmar.ejecutar(bolsa.getId().valor());
        assertThrows(ModificacionBolsaNoPermitidaException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        assertEquals(1,numeroPedidos()); assertEquals(1,stock());
    }
    @Test void revalidaStockYEliminacionAlConfirmar() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),3);
        prenda.actualizarStockVariante(new VariantePrendaId(variante()),1); prendas.guardarPrenda(prenda);
        // Act & Assert
        assertThrows(StockInsuficienteException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        assertEquals(1,stock()); assertEquals(0,numeroPedidos());
        ((JpaPrendaDeAutorRepository)prendas).eliminarLogicamente(prenda.getId());
        assertThrows(ReglaDominioException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
    }
    @Test void falloDespuesDeFlushReviertePedidoInventarioYBolsa() {
        // Arrange: se provoca el fallo DESPUÉS del INSERT y de todos los UPDATE reales.
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        doAnswer(inv -> { inv.callRealMethod(); throw new IllegalStateException("Fallo controlado después de persistir"); })
                .when(pedidoJpa).registrar(any());
        // Act & Assert
        var fallo=assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,
                () -> confirmar.ejecutar(bolsa.getId().valor()));
        assertEquals("Fallo controlado después de persistir",fallo.getMostSpecificCause().getMessage());
        assertEquals(0,numeroPedidos()); assertEquals(3,stock());
        assertFalse(bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow().isCerrada());
        assertEquals(0,jdbc.queryForObject("select count(*) from lineas_pedido",Integer.class));
    }
    @Test void reconstruccionDetectaTotalPersistidoCorrupto() {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),1);
        var p=confirmar.ejecutar(bolsa.getId().valor());
        jdbc.update("update pedidos_moda set total=1 where id=?",p.getId());
        // Act & Assert
        assertThrows(ReglaDominioException.class,() -> pedidos.porId(p.getId()));
    }
    private record Resultado(PedidoDeModa pedido,Throwable error) {}
    private List<Resultado> competir(UUID cliente1,UUID bolsa1,UUID cliente2,UUID bolsa2) throws Exception {
        var executor=Executors.newFixedThreadPool(2); var preparados=new CountDownLatch(2); var inicio=new CountDownLatch(1);
        try {
            List<Future<Resultado>> futures=new ArrayList<>();
            for(var datos:List.of(List.of(cliente1,bolsa1),List.of(cliente2,bolsa2))) futures.add(executor.submit(() -> {
                identidad(datos.getFirst()); preparados.countDown();
                try { if(!inicio.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Inicio no recibido");
                    return new Resultado(confirmar.ejecutar(datos.get(1)),null);
                } catch(Throwable e) { return new Resultado(null,e); }
                finally { SecurityContextHolder.clearContext(); }
            }));
            assertTrue(preparados.await(10,TimeUnit.SECONDS)); inicio.countDown();
            var resultados=new ArrayList<Resultado>(); for(var f:futures) resultados.add(f.get(20,TimeUnit.SECONDS));
            return resultados;
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }
    @Test void dosCompradoresConcurrentesNoSobrevendenLaUltimaExistencia() throws Exception {
        // Arrange: cada comprador pide dos unidades, sólo existen tres.
        var otro=UUID.randomUUID(); var segunda=bolsas.guardarBolsa(BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()),otro));
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        identidad(otro); agregar.ejecutar(segunda.getId().valor(),variante(),2);
        // Act
        var resultados=competir(comprador,bolsa.getId().valor(),otro,segunda.getId().valor());
        // Assert
        assertEquals(1,resultados.stream().filter(r -> r.pedido()!=null).count(),resultados.toString());
        assertEquals(1,resultados.stream().filter(r -> r.error() instanceof StockInsuficienteException).count(),resultados.toString());
        assertEquals(1,numeroPedidos()); assertEquals(1,stock());
    }
    @Test void confirmacionesConcurrentesDeMismaBolsaCreanUnSoloPedido() throws Exception {
        // Arrange
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        // Act
        var resultados=competir(comprador,bolsa.getId().valor(),comprador,bolsa.getId().valor());
        // Assert
        assertEquals(1,resultados.stream().filter(r -> r.pedido()!=null).count(),resultados.toString());
        assertEquals(1,resultados.stream().filter(r -> r.error() instanceof ModificacionBolsaNoPermitidaException).count(),resultados.toString());
        assertEquals(1,numeroPedidos()); assertEquals(1,stock());
    }

    @Test void falloDeUnaLineaNoDescuentaParcialmenteLasOtras() {
        // Arrange
        var otra=prenda("COP");
        agregar.ejecutar(bolsa.getId().valor(),variante(),2);
        agregar.ejecutar(bolsa.getId().valor(),otra.getVariantes().getFirst().id(),2);
        otra.actualizarStockVariante(new VariantePrendaId(otra.getVariantes().getFirst().id()),0);
        prendas.guardarPrenda(otra);
        // Act & Assert
        assertThrows(StockInsuficienteException.class,() -> confirmar.ejecutar(bolsa.getId().valor()));
        assertEquals(3,stock()); assertEquals(0,numeroPedidos());
        assertFalse(bolsas.obtenerBolsaPorId(bolsa.getId()).orElseThrow().isCerrada());
    }

    @Test void comprasConcurrentesConLineasEnOrdenInversoUsanBloqueoEstable() throws Exception {
        // Arrange: dos compradores añaden las mismas prendas en orden inverso.
        var otra=prenda("COP"); var otro=UUID.randomUUID();
        var segunda=bolsas.guardarBolsa(BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()),otro));
        agregar.ejecutar(bolsa.getId().valor(),variante(),1);
        agregar.ejecutar(bolsa.getId().valor(),otra.getVariantes().getFirst().id(),1);
        identidad(otro);
        agregar.ejecutar(segunda.getId().valor(),otra.getVariantes().getFirst().id(),1);
        agregar.ejecutar(segunda.getId().valor(),variante(),1);
        // Act
        var resultados=competir(comprador,bolsa.getId().valor(),otro,segunda.getId().valor());
        // Assert
        assertEquals(2,resultados.stream().filter(r -> r.pedido()!=null).count(),resultados.toString());
        assertEquals(2,numeroPedidos()); assertEquals(1,stock());
        assertEquals(1,prendas.obtenerPrendaPorId(otra.getId()).orElseThrow().getVariantes().getFirst().stockDisponible());
    }
}
