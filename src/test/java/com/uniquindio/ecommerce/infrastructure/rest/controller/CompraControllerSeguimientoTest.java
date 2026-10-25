package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.security.UsuarioActual;
import com.uniquindio.ecommerce.application.usecase.AgregarVarianteABolsaUseCase;
import com.uniquindio.ecommerce.application.usecase.ConfirmarPedidoUseCase;
import com.uniquindio.ecommerce.application.usecase.ConsultarPedidoUseCase;
import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.repository.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.config.JsonConfig;
import com.uniquindio.ecommerce.infrastructure.rest.exception.ApiExceptionHandler;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.*;
import com.uniquindio.ecommerce.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Punto 4: controller y caso de adición reales; se observan sus repositorios Mockito.
 * WithMockUser sólo delimita esta prueba MVC; el punto 1 verifica JWT reales por separado. */
@WebMvcTest(CompraController.class)
@WithMockUser(roles = "COMPRADOR")
@Import({AgregarVarianteABolsaUseCase.class, BolsaDeEstiloMapper.class, PedidoMapper.class,
        DineroMapper.class, ApiExceptionHandler.class, JsonConfig.class, SecurityConfig.class})
class CompraControllerSeguimientoTest {
    private static final UUID CLIENTE = UUID.fromString("c2000000-0000-4000-8000-000000000001");
    private static final UUID BOLSA = UUID.fromString("b2000000-0000-4000-8000-000000000001");
    private static final UUID VARIANTE = UUID.fromString("a2000000-0000-4000-8000-000000000001");
    private static final PrendaDeAutorId PRENDA = new PrendaDeAutorId(
            UUID.fromString("92000000-0000-4000-8000-000000000001"));
    private static final String RUTA = "/api/bolsas/" + BOLSA + "/items";
    private static final String JSON_DOS_UNIDADES =
            "{\"varianteId\":\"" + VARIANTE + "\",\"cantidad\":2}";

    @Autowired MockMvc mvc;
    @MockitoBean BolsaDeEstiloRepository bolsas;
    @MockitoBean PrendaDeAutorRepository prendas;
    @MockitoBean CatalogoVariantesRepository catalogo;
    @MockitoBean UsuarioActual actual;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean UsuarioRepository usuarios;
    // Colaboradores de las otras dos rutas del controller; no participan en POST items.
    @MockitoBean ConfirmarPedidoUseCase confirmar;
    @MockitoBean ConsultarPedidoUseCase consultar;

    @Test
    void agregarABolsaInexistenteDevuelve404SinSolicitarGuardado() throws Exception {
        // Arrange: el repositorio inyectado en el caso real informa ausencia.
        var id = new BolsaDeEstiloId(BOLSA);
        when(bolsas.obtenerBolsaParaActualizar(id)).thenReturn(Optional.empty());
        // Act
        var respuesta = mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_DOS_UNIDADES));
        // Assert
        respuesta.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("La bolsa no existe."))
                .andExpect(jsonPath("$.path").value(RUTA));
        verify(bolsas).obtenerBolsaParaActualizar(id);
        verify(bolsas, never()).guardarBolsa(any(BolsaDeEstilo.class));
        verify(prendas, never()).guardarPrenda(any(PrendaDeAutor.class));
        verifyNoInteractions(catalogo, actual);
    }

    @Test
    void agregarCantidadSuperiorAlStockDevuelve409SinSolicitarGuardado() throws Exception {
        // Arrange: agregado real con una unidad; petición válida de dos unidades.
        var bolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(BOLSA), CLIENTE);
        var prenda = PrendaDeAutor.crear(PRENDA, UUID.fromString("82000000-0000-4000-8000-000000000001"),
                "Camisa seguimiento", "Lino", new Dinero(new BigDecimal("19.95"), "COP"),
                List.of(VariantePrenda.crear(VARIANTE, new SKU("SEGUIMIENTO-M"), TipoTalla.M, 1)), 1);
        when(bolsas.obtenerBolsaParaActualizar(bolsa.getId())).thenReturn(Optional.of(bolsa));
        when(actual.id()).thenReturn(CLIENTE);
        when(catalogo.prendaDeVariante(VARIANTE)).thenReturn(Optional.of(PRENDA));
        when(prendas.obtenerPrendaParaActualizar(PRENDA)).thenReturn(Optional.of(prenda));
        // Act: el caso de uso llama realmente a BolsaDeEstilo.agregarLinea.
        var respuesta = mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_DOS_UNIDADES));
        // Assert: no se fabrica la excepción en un mock de caso de uso.
        respuesta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.message")
                        .value("No hay suficiente stock en el taller para esta variante de talla."))
                .andExpect(jsonPath("$.path").value(RUTA));
        verify(bolsas).obtenerBolsaParaActualizar(bolsa.getId());
        verify(actual).id();
        verify(catalogo).prendaDeVariante(VARIANTE);
        verify(prendas).obtenerPrendaParaActualizar(PRENDA);
        verify(bolsas, never()).guardarBolsa(any(BolsaDeEstilo.class));
        verify(prendas, never()).guardarPrenda(any(PrendaDeAutor.class));
        assertTrue(bolsa.getLineas().isEmpty());
        assertFalse(bolsa.isCerrada());
        assertEquals(1, prenda.getVariantes().getFirst().stockDisponible());
    }
}
