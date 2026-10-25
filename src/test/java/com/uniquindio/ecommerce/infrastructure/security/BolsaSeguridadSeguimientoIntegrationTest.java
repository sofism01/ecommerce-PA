package com.uniquindio.ecommerce.infrastructure.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Punto 1: misma URL y bolsa persistida, filtros y JWT reales, sin mocks. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seguimiento_seguridad;DB_CLOSE_DELAY=-1",
        "boheme.datos-iniciales.enabled=false",
        "boheme.usuarios-demo.enabled=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BolsaSeguridadSeguimientoIntegrationTest {
    private static final String PASSWORD = "SeguimientoFicticio2026!";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JwtDecoder decoder;
    @Autowired JdbcTemplate jdbc;

    private String rutaActiva;
    private String compradorToken;
    private String vendedorToken;
    private UUID compradorId;
    private UUID bolsaId;

    @BeforeAll
    void prepararUnaMismaBolsaYTokensDeLoginReales() throws Exception {
        // Arrange común: contexto H2 exclusivo, sin seed, y una sola bolsa.
        var comprador = registrar("comprador-seguimiento@boheme.test", "COMPRADOR");
        var vendedor = registrar("vendedor-seguimiento@boheme.test", "VENDEDOR");
        compradorId = UUID.fromString(comprador.get("id").asText());
        compradorToken = autenticarYVerificarJwt(comprador);
        vendedorToken = autenticarYVerificarJwt(vendedor);
        var bolsa = body(mvc.perform(post("/api/bolsas")
                .header("Authorization", "Bearer " + compradorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("clienteId", compradorId))))
                .andExpect(status().isCreated()).andReturn());
        bolsaId = UUID.fromString(bolsa.get("bolsaId").asText());
        rutaActiva = "/api/bolsas/activas/" + compradorId;
        comprobarBolsaOriginalIntacta();
    }

    @Test
    void consultarMismaBolsaSinTokenDevuelve401() throws Exception {
        // Arrange: la misma URL y el mismo recurso de la preparación común.
        String ruta = rutaActiva;
        // Act
        var respuesta = mvc.perform(get(ruta));
        // Assert
        respuesta.andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        comprobarBolsaOriginalIntacta();
    }

    @Test
    void consultarMismaBolsaConJwtVendedorValidoDevuelve403() throws Exception {
        // Arrange: JWT de login validado criptográficamente y rol VENDEDOR.
        String ruta = rutaActiva;
        String token = vendedorToken;
        assertEquals("VENDEDOR", decoder.decode(token).getClaimAsString("rol"));
        // Act
        var respuesta = mvc.perform(get(ruta).header("Authorization", "Bearer " + token));
        // Assert
        respuesta.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        comprobarBolsaOriginalIntacta();
    }

    @Test
    void consultarMismaBolsaConJwtCompradorPropietarioDevuelve200() throws Exception {
        // Arrange: JWT COMPRADOR cuyo subject coincide con el dueño persistido.
        String ruta = rutaActiva;
        String token = compradorToken;
        var jwt = decoder.decode(token);
        assertEquals("COMPRADOR", jwt.getClaimAsString("rol"));
        assertEquals(compradorId.toString(), jwt.getSubject());
        // Act
        var respuesta = mvc.perform(get(ruta).header("Authorization", "Bearer " + token));
        // Assert
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsaId").value(bolsaId.toString()))
                .andExpect(jsonPath("$.clienteId").value(compradorId.toString()))
                .andExpect(jsonPath("$.cerrada").value(false))
                .andExpect(jsonPath("$.lineas").isEmpty());
        comprobarBolsaOriginalIntacta();
    }

    private JsonNode registrar(String correo, String rol) throws Exception {
        return body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Cuenta seguimiento",
                        "correo", correo, "password", PASSWORD, "rol", rol))))
                .andExpect(status().isCreated()).andReturn());
    }

    private String autenticarYVerificarJwt(JsonNode usuario) throws Exception {
        var respuesta = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", usuario.get("correo").asText(),
                        "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn());
        String token = respuesta.get("token").asText();
        var jwt = decoder.decode(token); // Firma, emisor, tiempos y claims: decoder real.
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals("boheme", jwt.getClaimAsString("iss"));
        assertEquals(usuario.get("id").asText(), jwt.getSubject());
        assertEquals(usuario.get("correo").asText(), jwt.getClaimAsString("correo"));
        assertEquals(usuario.get("rol").asText(), jwt.getClaimAsString("rol"));
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertEquals("Bearer", respuesta.get("tokenType").asText());
        return token;
    }

    private void comprobarBolsaOriginalIntacta() {
        assertEquals(1, jdbc.queryForObject("select count(*) from bolsas_estilo", Integer.class));
        assertEquals(compradorId, jdbc.queryForObject(
                "select cliente_id from bolsas_estilo where id=?", UUID.class, bolsaId));
        assertFalse(jdbc.queryForObject("select cerrada from bolsas_estilo where id=?", Boolean.class, bolsaId));
        assertEquals(0, jdbc.queryForObject("select count(*) from lineas_bolsa where bolsa_id=?", Integer.class, bolsaId));
    }

    private JsonNode body(MvcResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
