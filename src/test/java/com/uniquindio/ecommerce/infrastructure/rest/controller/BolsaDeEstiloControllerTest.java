package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.infrastructure.config.JsonConfig;
import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.exception.BolsaDeEstiloNoEncontradaException;
import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.rest.exception.ApiExceptionHandler;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.BolsaDeEstiloMapper;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.DineroMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BolsaDeEstiloController.class)
@org.springframework.security.test.context.support.WithMockUser(roles="COMPRADOR")
@Import({com.uniquindio.ecommerce.infrastructure.security.SecurityConfig.class, BolsaDeEstiloMapper.class, DineroMapper.class, ApiExceptionHandler.class, JsonConfig.class})
class BolsaDeEstiloControllerTest {
    private static final UUID CLIENTE = UUID.fromString("22222222-2222-4222-8222-222222222222");
    @Autowired MockMvc mvc;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean com.uniquindio.ecommerce.domain.repository.UsuarioRepository usuarios;
    @MockitoBean AutorizarRecursosUseCase autorizar;
    @MockitoBean CrearBolsaDeEstiloUseCase crear;
    @MockitoBean ObtenerBolsaDeEstiloActivaUseCase obtenerActiva;
    @MockitoBean VaciarBolsaDeEstiloUseCase vaciar;
    @MockitoBean CerrarBolsaDeEstiloUseCase cerrar;

    private BolsaDeEstilo bolsa() {
        return BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), CLIENTE);
    }

    @Test
    void crear_Valida_Devuelve201ConDtoYDelega() throws Exception {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        when(crear.ejecutar(CLIENTE)).thenReturn(bolsa);
        // Act & Assert
        mvc.perform(post("/api/bolsas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\"" + CLIENTE + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bolsaId").value(bolsa.getId().valor().toString()))
                .andExpect(jsonPath("$.clienteId").value(CLIENTE.toString()))
                .andExpect(jsonPath("$.lineas").isEmpty()).andExpect(jsonPath("$.cerrada").value(false));
        verify(crear).ejecutar(CLIENTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"clienteId\":null}"})
    void crear_SinCliente_Devuelve400SinDelegar(String json) throws Exception {
        // Arrange: cliente ausente o nulo.
        // Act & Assert
        mvc.perform(post("/api/bolsas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.details.clienteId").isNotEmpty());
        verifyNoInteractions(crear);
    }

    @Test
    void obtenerActiva_DevuelveDtoConLineasYSubtotalDelDominio() throws Exception {
        // Arrange
        BolsaDeEstilo bolsa = bolsa();
        UUID variante = UUID.randomUUID();
        bolsa.agregarLinea(LineaDeBolsa.crear(variante, 2, new Dinero(new BigDecimal("50"), "COP")), 2);
        when(obtenerActiva.ejecutar(CLIENTE)).thenReturn(Optional.of(bolsa));
        // Act & Assert
        mvc.perform(get("/api/bolsas/activas/{id}", CLIENTE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsaId").value(bolsa.getId().valor().toString()))
                .andExpect(jsonPath("$.lineas[0].varianteId").value(variante.toString()))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(2))
                .andExpect(jsonPath("$.lineas[0].precioUnitario.monto").value(50))
                .andExpect(jsonPath("$.lineas[0].subtotal.monto").value(100))
                .andExpect(jsonPath("$.lineas[0].subtotal.moneda").value("COP"));
        verify(obtenerActiva).ejecutar(CLIENTE);
    }

    @Test
    void obtenerActiva_Ausente_Devuelve404ConContratoDeError() throws Exception {
        // Arrange
        when(obtenerActiva.ejecutar(CLIENTE)).thenReturn(Optional.empty());
        // Act & Assert
        mvc.perform(get("/api/bolsas/activas/{id}", CLIENTE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/bolsas/activas/" + CLIENTE));
    }

    @Test
    void vaciar_Devuelve204YDelegaIdentificadorTipado() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        // Act & Assert
        mvc.perform(delete("/api/bolsas/{id}/items", id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(vaciar).ejecutar(new BolsaDeEstiloId(id));
    }

    @Test
    void cerrar_Devuelve204YDelegaIdentificadorTipado() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        // Act & Assert
        mvc.perform(patch("/api/bolsas/{id}/cerrar", id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(cerrar).ejecutar(new BolsaDeEstiloId(id));
    }

    @Test
    void vaciarYCerrar_BolsaInexistente_Devuelven404() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        doThrow(new BolsaDeEstiloNoEncontradaException("La bolsa de estilo no existe.")).when(vaciar).ejecutar(new BolsaDeEstiloId(id));
        doThrow(new BolsaDeEstiloNoEncontradaException("La bolsa de estilo no existe.")).when(cerrar).ejecutar(new BolsaDeEstiloId(id));
        // Act & Assert
        mvc.perform(delete("/api/bolsas/{id}/items", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
        mvc.perform(patch("/api/bolsas/{id}/cerrar", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void vaciar_BolsaCerrada_Devuelve409() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        doThrow(new ModificacionBolsaNoPermitidaException("Una bolsa cerrada no puede ser modificada."))
                .when(vaciar).ejecutar(new BolsaDeEstiloId(id));
        // Act & Assert
        mvc.perform(delete("/api/bolsas/{id}/items", id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.code").value("BOLSA_CERRADA"));
    }

    @Test
    void clienteUuidInvalido_Devuelve400() throws Exception {
        // Arrange: UUID inválido en JSON y en la ruta.
        // Act & Assert
        mvc.perform(post("/api/bolsas").contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":\"invalido\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        mvc.perform(get("/api/bolsas/activas/invalido")).andExpect(status().isBadRequest());
        verifyNoInteractions(crear, obtenerActiva);
    }
}
