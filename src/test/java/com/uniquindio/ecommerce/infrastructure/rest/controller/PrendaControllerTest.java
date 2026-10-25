package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.infrastructure.config.JsonConfig;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.rest.exception.ApiExceptionHandler;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.DineroMapper;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.PrendaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrendaController.class)
@org.springframework.security.test.context.support.WithMockUser(roles="VENDEDOR")
@Import({com.uniquindio.ecommerce.infrastructure.security.SecurityConfig.class, PrendaMapper.class, DineroMapper.class, com.uniquindio.ecommerce.infrastructure.rest.mapper.ConsultaPrendasMapper.class, ApiExceptionHandler.class, JsonConfig.class})
class PrendaControllerTest {
    private static final UUID CREADOR = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String REQUEST = """
            {"creadorId":"11111111-1111-4111-8111-111111111111","nombre":"Camisa","descripcion":"Lino","precioBase":150000,"moneda":"COP","variantes":[{"sku":"cam-m","tipoTalla":"M","stockDisponible":5}],"stockTotal":5}
            """;

    @Autowired MockMvc mvc;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean com.uniquindio.ecommerce.domain.repository.UsuarioRepository usuarios;
    @MockitoBean AutorizarRecursosUseCase autorizar;
    @MockitoBean RegistrarPrendaDeAutorUseCase registrar;
    @MockitoBean ListarPrendasPorCreadorUseCase listar;
    @MockitoBean BuscarPrendasUseCase buscar;
    @MockitoBean SuspenderVentaPrendaDeAutorUseCase suspender;
    @MockitoBean ActualizarStockVariantePrendaUseCase actualizarStock;

    @Test
    void buscar_DevuelvePaginaYDelegaFiltros() throws Exception {
        // Arrange
        var consulta = new com.uniquindio.ecommerce.domain.query.ConsultaPrendas("LINO", CREADOR, "COP", 1, 2);
        when(buscar.ejecutar(consulta)).thenReturn(new com.uniquindio.ecommerce.domain.query.Pagina<>(List.of(prenda()), 3, 2, 1, 2));
        // Act & Assert
        mvc.perform(get("/api/prendas").param("q", "LINO").param("creadorId", CREADOR.toString())
                .param("moneda", "COP").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nombre").value("Camisa"))
                .andExpect(jsonPath("$.content[0].variantes", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(2));
        verify(buscar).ejecutar(consulta);
    }

    @Test
    void buscar_SinParametrosUsaValoresPorDefectoYDevuelve200Vacio() throws Exception {
        // Arrange
        var consulta = new com.uniquindio.ecommerce.domain.query.ConsultaPrendas(null, null, null, 0, 20);
        when(buscar.ejecutar(consulta)).thenReturn(new com.uniquindio.ecommerce.domain.query.Pagina<>(List.of(), 0, 0, 0, 20));
        // Act & Assert
        mvc.perform(get("/api/prendas")).andExpect(status().isOk())
                .andExpect(content().json("{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"page\":0,\"size\":20}"));
        verify(buscar).ejecutar(consulta);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "page,-1", "size,0", "size,-1", "size,101", "page,abc", "size,1.5",
            "page,2147483647", "page,2147483648", "creadorId,invalido", "moneda,CO", "moneda,123"})
    void buscar_ParametrosInvalidosDevuelven400SinDelegar(String parametro, String valor) throws Exception {
        // Arrange: parámetro de consulta inválido.
        // Act & Assert
        mvc.perform(get("/api/prendas").param(parametro, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        verifyNoInteractions(buscar);
    }

    private PrendaDeAutor prenda() {
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), CREADOR, "Camisa", "Lino",
                new Dinero(new BigDecimal("150000"), "COP"),
                List.of(VariantePrenda.crear(UUID.randomUUID(), new SKU("cam-m"), TipoTalla.M, 5)), 5);
    }

    @Test
    void registrar_Valido_Devuelve201YDelegaConDominioMapeado() throws Exception {
        // Arrange
        when(registrar.ejecutar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.prendaId").isString())
                .andExpect(jsonPath("$.creadorId").value(CREADOR.toString()))
                .andExpect(jsonPath("$.precioBase.monto").value(150000))
                .andExpect(jsonPath("$.precioBase.moneda").value("COP"))
                .andExpect(jsonPath("$.variantes[0].varianteId").isString())
                .andExpect(jsonPath("$.variantes[0].sku").value("CAM-M"))
                .andExpect(jsonPath("$.variantes[0].tipoTalla").value("M"))
                .andExpect(jsonPath("$.variantes[0].stockDisponible").value(5))
                .andExpect(jsonPath("$.stockTotal").value(5))
                .andExpect(jsonPath("$.disponibleParaLaVenta").value(true));
        var captor = ArgumentCaptor.forClass(PrendaDeAutor.class);
        verify(registrar).ejecutar(captor.capture());
        assertEquals(CREADOR, captor.getValue().getCreadorId());
        assertEquals("CAM-M", captor.getValue().getVariantes().get(0).sku().value());
        assertNotNull(captor.getValue().getId());
    }

    static Stream<Arguments> solicitudesInvalidas() {
        return Stream.of(
                Arguments.of(REQUEST.replace("\"Camisa\"", "\" \""), "nombre"),
                Arguments.of(REQUEST.replace("\"precioBase\":150000", "\"precioBase\":0"), "precioBase"),
                Arguments.of(REQUEST.replace("\"precioBase\":150000", "\"precioBase\":null"), "precioBase"),
                Arguments.of(REQUEST.replace("\"COP\"", "\"cop\""), "moneda"),
                Arguments.of(REQUEST.replace("\"sku\":\"cam-m\"", "\"sku\":\" \""), "variantes[0].sku"),
                Arguments.of(REQUEST.replace("\"stockDisponible\":5", "\"stockDisponible\":-1"), "variantes[0].stockDisponible"),
                Arguments.of(REQUEST.replace("\"stockDisponible\":5", "\"stockDisponible\":null"), "variantes[0].stockDisponible"),
                Arguments.of(REQUEST.replace("\"tipoTalla\":\"M\"", "\"tipoTalla\":null"), "variantes[0].tipoTalla"),
                Arguments.of(REQUEST.replace("\"stockTotal\":5", "\"stockTotal\":-1"), "stockTotal"),
                Arguments.of(REQUEST.replace(",\"stockTotal\":5", ""), "stockTotal"));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void registrar_Invalido_Devuelve400ConDetalleSinDelegar(String json, String campo) throws Exception {
        // Arrange: solicitud inválida del proveedor.
        // Act & Assert
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.path").value("/api/prendas"))
                .andExpect(jsonPath("$.details['" + campo + "']").isNotEmpty());
        verifyNoInteractions(registrar, listar, suspender, actualizarStock);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "null"})
    void registrar_ListaVariantesInvalida_Devuelve400(String variantes) throws Exception {
        // Arrange
        String json = REQUEST.replace("[{\"sku\":\"cam-m\",\"tipoTalla\":\"M\",\"stockDisponible\":5}]", variantes);
        // Act & Assert
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        verifyNoInteractions(registrar);
    }

    @Test
    void listar_DevuelveColeccionCompletaYDelega() throws Exception {
        // Arrange
        PrendaDeAutor primera = prenda();
        PrendaDeAutor segunda = prenda();
        when(listar.ejecutar(CREADOR)).thenReturn(List.of(primera, segunda));
        // Act & Assert
        mvc.perform(get("/api/prendas/creador/{id}", CREADOR)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].prendaId").value(primera.getId().value().toString()))
                .andExpect(jsonPath("$[1].prendaId").value(segunda.getId().value().toString()));
        verify(listar).ejecutar(CREADOR);
    }

    @Test
    void listar_SinResultados_Devuelve200YColeccionVacia() throws Exception {
        // Arrange
        when(listar.ejecutar(CREADOR)).thenReturn(List.of());
        // Act & Assert
        mvc.perform(get("/api/prendas/creador/{id}", CREADOR)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void suspender_Devuelve204YDelegaIdentificadorTipado() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/suspender", id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(suspender).ejecutar(new PrendaDeAutorId(id));
    }

    @Test
    void suspender_Inexistente_Devuelve404() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        doThrow(new RecursoNoEncontradoException("La prenda de autor no existe.")).when(suspender).ejecutar(new PrendaDeAutorId(id));
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/suspender", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.path").value("/api/prendas/" + id + "/suspender"));
    }

    @Test
    void suspender_ReglaIncumplida_Devuelve409() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        doThrow(new ReglaDominioException("La prenda ya está suspendida para la venta.")).when(suspender).ejecutar(new PrendaDeAutorId(id));
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/suspender", id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGLA_DOMINIO"))
                .andExpect(jsonPath("$.message").value("La prenda ya está suspendida para la venta."));
    }

    @Test
    void actualizarStock_Valido_Devuelve204YDelegaTodosLosArgumentos() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID variante = UUID.randomUUID();
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/variantes/{variante}/stock", id, variante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nuevoStock\":0}"))
                .andExpect(status().isNoContent());
        verify(actualizarStock).ejecutar(new PrendaDeAutorId(id), new VariantePrendaId(variante), 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nuevoStock\":null}", "{\"nuevoStock\":-1}"})
    void actualizarStock_Invalido_Devuelve400SinDelegar(String json) throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/variantes/{variante}/stock", id, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.nuevoStock").isNotEmpty());
        verifyNoInteractions(actualizarStock);
    }

    @Test
    void actualizarStock_VarianteInexistente_Devuelve404() throws Exception {
        // Arrange
        doThrow(new RecursoNoEncontradoException("La variante especificada no existe."))
                .when(actualizarStock).ejecutar(any(), any(), anyInt());
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/variantes/{variante}/stock", UUID.randomUUID(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nuevoStock\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void actualizarStock_Fraccionario_Devuelve400SinTruncarNiDelegar() throws Exception {
        // Arrange: una cantidad fraccionaria no es una existencia entera.
        // Act & Assert
        mvc.perform(patch("/api/prendas/{id}/variantes/{variante}/stock", UUID.randomUUID(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nuevoStock\":1.5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        verifyNoInteractions(actualizarStock);
    }

    @ParameterizedTest
    @ValueSource(strings = {"stockTotal", "stockDisponible", "tipoTalla"})
    void registrar_RechazaFraccionesYOrdinalesDeEnum(String campo) throws Exception {
        // Arrange
        String json = switch (campo) {
            case "stockTotal" -> REQUEST.replace("\"stockTotal\":5", "\"stockTotal\":1.5");
            case "stockDisponible" -> REQUEST.replace("\"stockDisponible\":5", "\"stockDisponible\":1.5");
            default -> REQUEST.replace("\"tipoTalla\":\"M\"", "\"tipoTalla\":1");
        };
        // Act & Assert
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        verifyNoInteractions(registrar);
    }

    @Test
    void uuidInvalido_Devuelve400SinInvocarCasoDeUso() throws Exception {
        // Arrange: UUID mal formado en la ruta.
        // Act & Assert
        mvc.perform(patch("/api/prendas/no-es-uuid/suspender")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        verifyNoInteractions(suspender);
    }

    @Test
    void jsonMalFormadoYTallaDesconocida_Devuelven400() throws Exception {
        // Arrange
        String tallaDesconocida = REQUEST.replace("\"tipoTalla\":\"M\"", "\"tipoTalla\":\"XXXL\"");
        // Act & Assert
        for (String json : List.of("{", tallaDesconocida)) {
            mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
        }
        verifyNoInteractions(registrar);
    }

    @Test
    void erroresDeProtocolo_ConservanEstadoYContratoJson() throws Exception {
        // Arrange: método, contenido y ruta no admitidos.
        // Act & Assert
        mvc.perform(put("/api/prendas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METODO_NO_PERMITIDO"));
        mvc.perform(post("/api/prendas").contentType(MediaType.TEXT_PLAIN).content(REQUEST))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("TIPO_CONTENIDO_NO_ADMITIDO"));
        mvc.perform(get("/api/no-existe")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void errorInesperado_Devuelve500SinFiltrarDetalles() throws Exception {
        // Arrange
        when(listar.ejecutar(CREADOR)).thenThrow(new IllegalStateException("password=secreto-interno"));
        // Act & Assert
        mvc.perform(get("/api/prendas/creador/{id}", CREADOR)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(jsonPath("$.details").isMap())
                .andExpect(content().string(not(containsString("secreto-interno"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
