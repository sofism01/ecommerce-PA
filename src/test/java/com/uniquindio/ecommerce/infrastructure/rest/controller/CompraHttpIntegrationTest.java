package com.uniquindio.ecommerce.infrastructure.rest.controller;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Registro/login y filtros JWT reales, casos de uso y H2 sin mocks. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:compra_http;DB_CLOSE_DELAY=-1","boheme.datos-iniciales.enabled=false"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompraHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    JsonNode vendedor,comprador,ajeno;
    String vendedorToken,compradorToken,ajenoToken;
    static final String PASSWORD="CompraFicticia2026!";
    private JsonNode body(MvcResult r) { return json.readTree(r.getResponse().getContentAsByteArray()); }
    private JsonNode registrar(String rol) throws Exception {
        return body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("nombre","Cuenta prueba compra","correo",UUID.randomUUID()+"@boheme.test","password",PASSWORD,"rol",rol))))
            .andExpect(status().isCreated()).andReturn());
    }
    private String login(JsonNode usuario) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo",usuario.get("correo").asText(),"password",PASSWORD))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }
    @BeforeAll void cuentasGenerales() throws Exception {
        vendedor=registrar("VENDEDOR"); vendedorToken=login(vendedor);
        ajeno=registrar("COMPRADOR"); ajenoToken=login(ajeno);
    }
    @BeforeEach void compradorAislado() throws Exception {
        comprador=registrar("COMPRADOR"); compradorToken=login(comprador);
    }
    private JsonNode crearPrenda() throws Exception {
        return body(mvc.perform(post("/api/prendas").header("Authorization","Bearer "+vendedorToken).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("creadorId",vendedor.get("id").asText(),"nombre","Camisa HTTP",
                "descripcion","Colección de prueba","precioBase",19.95,"moneda","COP","stockTotal",3,
                "variantes",List.of(Map.of("sku","HTTP-M","tipoTalla","M","stockDisponible",3))))))
            .andExpect(status().isCreated()).andReturn());
    }
    private String crearBolsa() throws Exception {
        return body(mvc.perform(post("/api/bolsas").header("Authorization","Bearer "+compradorToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\""+comprador.get("id").asText()+"\"}"))
                .andExpect(status().isCreated()).andReturn()).get("bolsaId").asText();
    }
    private void agregar(String bolsa,JsonNode prenda,int cantidad) throws Exception {
        mvc.perform(post("/api/bolsas/"+bolsa+"/items").header("Authorization","Bearer "+compradorToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("varianteId",prenda.get("variantes").get(0).get("varianteId").asText(),"cantidad",cantidad))))
                .andExpect(status().isOk());
    }
    @Test void flujoCompletoConErrorIntencionalStockPedidoYRelecturaDesdeH2() throws Exception {
        // Arrange: catálogo y bolsa creados por sus propietarios autenticados.
        var prenda=crearPrenda(); String bolsa=crearBolsa();
        String variante=prenda.get("variantes").get(0).get("varianteId").asText();
        long pedidosAntes=jdbc.queryForObject("select count(*) from pedidos_moda",Long.class);
        // Act & Assert: error de negocio sin efectos secundarios.
        mvc.perform(post("/api/bolsas/"+bolsa+"/items").header("Authorization","Bearer "+compradorToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("varianteId",variante,"cantidad",4))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STOCK_INSUFICIENTE"));
        assertEquals(3,jdbc.queryForObject("select stock_disponible from variantes_prenda where id=?",Integer.class,UUID.fromString(variante)));
        assertEquals(pedidosAntes,jdbc.queryForObject("select count(*) from pedidos_moda",Long.class));
        // Act: intento válido; los valores monetarios enviados como campos extra no deciden el precio.
        mvc.perform(post("/api/bolsas/"+bolsa+"/items").header("Authorization","Bearer "+compradorToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("varianteId",variante,"cantidad",2,"precioUnitario",0.01,"moneda","USD","stock",999))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lineas[0].precioUnitario.monto").value(19.95));
        mvc.perform(get("/api/bolsas/activas/"+comprador.get("id").asText()).header("Authorization","Bearer "+compradorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lineas[0].subtotal.monto").value(39.90));
        var pedido=body(mvc.perform(post("/api/bolsas/"+bolsa+"/confirmar-pedido").header("Authorization","Bearer "+compradorToken))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn());
        // Assert: consulta del histórico, persistencia y descuento real.
        mvc.perform(get("/api/pedidos/"+pedido.get("pedidoId").asText()).header("Authorization","Bearer "+compradorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(2)).andExpect(jsonPath("$.total.monto").value(39.90));
        assertEquals(1,jdbc.queryForObject("select count(*) from pedidos_moda where id=?",Integer.class,UUID.fromString(pedido.get("pedidoId").asText())));
        assertEquals(1,jdbc.queryForObject("select stock_disponible from variantes_prenda where id=?",Integer.class,UUID.fromString(variante)));
        mvc.perform(post("/api/bolsas/"+bolsa+"/confirmar-pedido").header("Authorization","Bearer "+compradorToken)).andExpect(status().isConflict());
    }
    @ParameterizedTest
    @CsvSource({"POST,/api/bolsas/00000000-0000-0000-0000-000000000001/items",
            "POST,/api/bolsas/00000000-0000-0000-0000-000000000001/confirmar-pedido",
            "GET,/api/pedidos/00000000-0000-0000-0000-000000000001"})
    void rutasNuevasExigenJwtYRolComprador(String metodo,String path) throws Exception {
        // Arrange
        var method=HttpMethod.valueOf(metodo);
        // Act & Assert
        mvc.perform(request(method,path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mvc.perform(request(method,path).header("Authorization","Bearer "+vendedorToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }
    @Test void compradorAjenoNoPuedeAgregarConfirmarNiConsultarElPedido() throws Exception {
        // Arrange
        var prenda=crearPrenda(); String bolsa=crearBolsa();
        // Act & Assert
        mvc.perform(post("/api/bolsas/"+bolsa+"/items").header("Authorization","Bearer "+ajenoToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("varianteId",prenda.get("variantes").get(0).get("varianteId").asText(),"cantidad",1))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bolsas/"+bolsa+"/confirmar-pedido").header("Authorization","Bearer "+ajenoToken)).andExpect(status().isForbidden());
        agregar(bolsa,prenda,1);
        var p=body(mvc.perform(post("/api/bolsas/"+bolsa+"/confirmar-pedido").header("Authorization","Bearer "+compradorToken))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/pedidos/"+p.get("pedidoId").asText()).header("Authorization","Bearer "+ajenoToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/pedidos/"+UUID.randomUUID()).header("Authorization","Bearer "+compradorToken)).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"varianteId\":null,\"cantidad\":1}",
            "{\"varianteId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":0}",
            "{\"varianteId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":-1}",
            "{\"varianteId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":1.5}",
            "{\"varianteId\":\"invalido\",\"cantidad\":1}"})
    void cantidadesYUuidInvalidosDevuelven400(String contenido) throws Exception {
        // Arrange
        String bolsa=crearBolsa();
        // Act & Assert
        mvc.perform(post("/api/bolsas/"+bolsa+"/items").header("Authorization","Bearer "+compradorToken)
                .contentType(MediaType.APPLICATION_JSON).content(contenido)).andExpect(status().isBadRequest());
    }
    @Test void swaggerDocumentaTresOperacionesConBearerYDtoSinPrecioEnSolicitud() throws Exception {
        // Arrange & Act
        var api=body(mvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn());
        // Assert
        assertTrue(api.at("/paths/~1api~1bolsas~1{bolsaId}~1items/post/security/0").has("bearerAuth"));
        assertTrue(api.at("/paths/~1api~1pedidos~1{pedidoId}/get/security/0").has("bearerAuth"));
        var campos=api.at("/components/schemas/AgregarItemBolsaRequest/properties");
        assertEquals(2,campos.size()); assertTrue(campos.has("varianteId")); assertTrue(campos.has("cantidad"));
        assertTrue(api.at("/components/schemas/PedidoResponse/properties").has("total"));
    }
}
