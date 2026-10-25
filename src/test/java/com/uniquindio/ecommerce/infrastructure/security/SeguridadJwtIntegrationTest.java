package com.uniquindio.ecommerce.infrastructure.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Filtros reales, firma real, BCrypt real y persistencia H2, sin mocks de seguridad. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:jwt_integration;DB_CLOSE_DELAY=-1",
        "boheme.datos-iniciales.enabled=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SeguridadJwtIntegrationTest {
    static final String PASSWORD="PruebaSegura2026!";
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired BCryptContrasenas passwords;
    JsonNode vendedor, comprador, otroVendedor, otroComprador;
    String vendedorToken, compradorToken, otroVendedorToken, otroCompradorToken;

    @BeforeAll
    void cuentasPersistidas() throws Exception {
        vendedor=registrar("vendedor@boheme.test","VENDEDOR");
        comprador=registrar("comprador@boheme.test","COMPRADOR");
        otroVendedor=registrar("otro-vendedor@boheme.test","VENDEDOR");
        otroComprador=registrar("otro-comprador@boheme.test","COMPRADOR");
        vendedorToken=login("vendedor@boheme.test",PASSWORD).get("token").asText();
        compradorToken=login("comprador@boheme.test",PASSWORD).get("token").asText();
        otroVendedorToken=login("otro-vendedor@boheme.test",PASSWORD).get("token").asText();
        otroCompradorToken=login("otro-comprador@boheme.test",PASSWORD).get("token").asText();
    }
    private JsonNode body(MvcResult result) { return json.readTree(result.getResponse().getContentAsByteArray()); }
    private JsonNode registrar(String correo,String rol) throws Exception {
        return body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre","Cuenta prueba","correo",correo,"password",PASSWORD,"rol",rol))))
                .andExpect(status().isCreated()).andReturn());
    }
    private JsonNode login(String correo,String password) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo",correo,"password",password))))
                .andExpect(status().isOk()).andReturn());
    }
    private String prendaJson(String propietario) {
        return json.writeValueAsString(Map.of("creadorId",propietario,"nombre","Camisa JWT","descripcion","Lino",
                "precioBase",30,"moneda","COP","stockTotal",3,
                "variantes",List.of(Map.of("sku","JWT-M","tipoTalla","M","stockDisponible",3))));
    }
    private JsonNode crearPrenda() throws Exception {
        return body(mvc.perform(post("/api/prendas").header("Authorization","Bearer "+vendedorToken)
                .contentType(MediaType.APPLICATION_JSON).content(prendaJson(vendedor.get("id").asText())))
                .andExpect(status().isCreated()).andReturn());
    }
    private JsonNode crearBolsa() throws Exception {
        return body(mvc.perform(post("/api/bolsas").header("Authorization","Bearer "+compradorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":\""+comprador.get("id").asText()+"\"}"))
                .andExpect(status().isCreated()).andReturn());
    }
    private String firmado(Instant expedicion, Instant expiracion, String subject, String rol) {
        var builder=JwtClaimsSet.builder().issuer("boheme").subject(subject).issuedAt(expedicion)
                .claim("correo",vendedor.get("correo").asText()).claim("rol",rol);
        if (expiracion != null) builder.expiresAt(expiracion);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),builder.build())).getTokenValue();
    }

    @Test void registroPersisteHashBCryptYNoExponePassword() throws Exception {
        // Arrange
        String correo="nueva@boheme.test";
        // Act
        var usuario=registrar(correo,"COMPRADOR");
        String hash=jdbc.queryForObject("select password_hash from usuarios where correo=?",String.class,correo);
        // Assert
        assertTrue(hash.startsWith("$2a$12$"));
        assertTrue(passwords.verificar(PASSWORD,hash));
        assertFalse(usuario.has("password")); assertFalse(usuario.has("passwordHash"));
        assertEquals("COMPRADOR",usuario.get("rol").asText());
        assertTrue(usuario.get("activo").asBoolean());
        UUID.fromString(usuario.get("id").asText());
    }

    @Test void registroDuplicadoNormalizaCorreoYDevuelve409() throws Exception {
        // Arrange: cuenta ya persistida en BeforeAll.
        // Act & Assert
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre","Duplicado","correo","VENDEDOR@BOHEME.TEST","password",PASSWORD,"rol","COMPRADOR"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CORREO_DUPLICADO"));
    }

    @ParameterizedTest
    @CsvSource({"nombre,''", "correo,no-es-correo", "password,corta", "rol,ADMIN", "rol,ROLE_VENDEDOR", "nombre,' '", "password,' '"})
    void registroInvalidoDevuelve400(String campo,String valor) throws Exception {
        // Arrange
        var datos=new HashMap<String,Object>(Map.of("nombre","Prueba","correo","validacion@boheme.test","password",PASSWORD,"rol","COMPRADOR"));
        datos.put(campo,valor);
        // Act & Assert
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATOS_INVALIDOS"));
    }

    @Test void registroRechazaMasDe72BytesUtf8YNumerosComoRol() throws Exception {
        // Arrange
        String unicode="ñ".repeat(40);
        // Act & Assert
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre","Cuenta","correo","unicode@boheme.test","password",unicode,"rol","COMPRADOR"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cuenta\",\"correo\":\"enum@boheme.test\",\"password\":\"PruebaSegura2026!\",\"rol\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test void loginDevuelveBearerExpiracionYClaimsCorrectos() throws Exception {
        // Arrange
        String correo="VENDEDOR@BOHEME.TEST";
        // Act
        var respuesta=login(correo,PASSWORD);
        var jwt=decoder.decode(respuesta.get("token").asText());
        // Assert
        assertEquals("Bearer",respuesta.get("tokenType").asText());
        assertEquals(3600,respuesta.get("expiresIn").asLong());
        assertEquals(vendedor.get("id").asText(),jwt.getSubject());
        assertEquals("vendedor@boheme.test",jwt.getClaimAsString("correo"));
        assertEquals("VENDEDOR",jwt.getClaimAsString("rol"));
        assertNotNull(jwt.getExpiresAt());
        assertEquals(jwt.getExpiresAt(),Instant.parse(respuesta.get("expiresAt").asText()));
        assertEquals(3600,jwt.getExpiresAt().getEpochSecond()-jwt.getIssuedAt().getEpochSecond());
        assertFalse(jwt.getClaims().containsKey("password"));
        assertFalse(respuesta.get("usuario").has("passwordHash"));
    }

    @ParameterizedTest @ValueSource(strings={"vendedor@boheme.test","inexistente@boheme.test"})
    void loginIncorrectoNoRevelaExistenciaDelCorreo(String correo) throws Exception {
        // Arrange
        String datos=json.writeValueAsString(Map.of("correo",correo,"password","Incorrecta2026!"));
        // Act & Assert
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(datos))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate","Bearer"))
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.message").value("Credenciales o token inválidos."))
                .andExpect(jsonPath("$.details").isMap()).andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test void tokenExpiradoDevuelve401() throws Exception {
        // Arrange
        var ahora=Instant.now();
        String token=firmado(ahora.minusSeconds(100),ahora.minusSeconds(1),vendedor.get("id").asText(),"VENDEDOR");
        // Act & Assert
        mvc.perform(post("/api/prendas").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test void tokenConFirmaInvalidaDevuelve401() throws Exception {
        // Arrange: una clave diferente, sin cambiar los claims.
        var distinto=NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32],"HmacSHA256")).build();
        var original=decoder.decode(vendedorToken);
        String token=distinto.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().claims(c -> c.putAll(original.getClaims())).build())).getTokenValue();
        // Act & Assert
        mvc.perform(post("/api/prendas").header("Authorization","Bearer "+token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @ParameterizedTest @ValueSource(strings={"sin-expiracion","rol-invalido","usuario-inexistente","sub-invalido","expedicion-futura"})
    void rechazaClaimsInvalidosAunqueFirmaSeaValida(String caso) throws Exception {
        // Arrange
        var ahora=Instant.now();
        String token=firmado(caso.equals("expedicion-futura")?ahora.plusSeconds(300):ahora.minusSeconds(1),
                caso.equals("sin-expiracion")?null:ahora.plusSeconds(3600),
                caso.equals("usuario-inexistente")?UUID.randomUUID().toString():caso.equals("sub-invalido")?"invalido":vendedor.get("id").asText(),
                caso.equals("rol-invalido")?"ADMIN":"VENDEDOR");
        // Act & Assert
        mvc.perform(post("/api/prendas").header("Authorization","Bearer "+token))
                .andExpect(status().isUnauthorized());
    }

    @Test void cuentaInactivaRechazaLoginYTokenPrevio() throws Exception {
        // Arrange
        jdbc.update("update usuarios set activo=false where id=?",UUID.fromString(vendedor.get("id").asText()));
        // Act & Assert
        mvc.perform(post("/api/prendas").header("Authorization","Bearer "+vendedorToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo","vendedor@boheme.test","password",PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/prendas", "PATCH,/api/prendas/00000000-0000-0000-0000-000000000001/suspender",
        "PATCH,/api/prendas/00000000-0000-0000-0000-000000000001/variantes/00000000-0000-0000-0000-000000000002/stock",
        "POST,/api/bolsas", "GET,/api/bolsas/activas/00000000-0000-0000-0000-000000000001",
        "DELETE,/api/bolsas/00000000-0000-0000-0000-000000000001/items", "PATCH,/api/bolsas/00000000-0000-0000-0000-000000000001/cerrar"})
    void todasLasRutasProtegidasExigenTokenYRol(String metodo,String path) throws Exception {
        // Arrange
        var method=org.springframework.http.HttpMethod.valueOf(metodo);
        String incorrecto=path.startsWith("/api/prendas")?compradorToken:vendedorToken;
        // Act & Assert
        mvc.perform(request(method,path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mvc.perform(request(method,path).header("Authorization","Bearer "+incorrecto))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test void vendedorOperaSuPrendaYNoPuedeSuplantarCreador() throws Exception {
        // Arrange
        var prenda=crearPrenda();
        String path="/api/prendas/"+prenda.get("prendaId").asText();
        // Act & Assert
        mvc.perform(patch(path+"/variantes/"+prenda.get("variantes").get(0).get("varianteId").asText()+"/stock")
                .header("Authorization","Bearer "+vendedorToken).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoStock\":2}"))
                .andExpect(status().isNoContent());
        mvc.perform(patch(path+"/suspender").header("Authorization","Bearer "+vendedorToken)).andExpect(status().isNoContent());
        mvc.perform(post("/api/prendas").header("Authorization","Bearer "+vendedorToken)
                .contentType(MediaType.APPLICATION_JSON).content(prendaJson(otroVendedor.get("id").asText())))
                .andExpect(status().isForbidden());
        em.flush();
        assertEquals(false,jdbc.queryForObject("select disponible_para_venta from prendas_autor where id=?",Boolean.class,UUID.fromString(prenda.get("prendaId").asText())));
    }

    @Test void vendedorNoPuedeModificarPrendaAjena() throws Exception {
        // Arrange
        var prenda=crearPrenda();
        String path="/api/prendas/"+prenda.get("prendaId").asText();
        // Act & Assert
        mvc.perform(patch(path+"/suspender").header("Authorization","Bearer "+otroVendedorToken)).andExpect(status().isForbidden());
        mvc.perform(patch(path+"/variantes/"+prenda.get("variantes").get(0).get("varianteId").asText()+"/stock")
                .header("Authorization","Bearer "+otroVendedorToken).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoStock\":0}"))
                .andExpect(status().isForbidden());
        em.flush();
        assertEquals(3,jdbc.queryForObject("select stock_disponible from variantes_prenda where prenda_id=?",Integer.class,UUID.fromString(prenda.get("prendaId").asText())));
    }

    @Test void compradorOperaSuBolsaYNoPuedeSuplantarCliente() throws Exception {
        // Arrange
        var bolsa=crearBolsa(); String id=bolsa.get("bolsaId").asText();
        // Act & Assert
        mvc.perform(get("/api/bolsas/activas/"+comprador.get("id").asText()).header("Authorization","Bearer "+compradorToken))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/bolsas/"+id+"/items").header("Authorization","Bearer "+compradorToken)).andExpect(status().isNoContent());
        mvc.perform(patch("/api/bolsas/"+id+"/cerrar").header("Authorization","Bearer "+compradorToken)).andExpect(status().isNoContent());
        mvc.perform(post("/api/bolsas").header("Authorization","Bearer "+compradorToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\""+otroComprador.get("id").asText()+"\"}")).andExpect(status().isForbidden());
    }

    @Test void compradorNoPuedeConsultarVaciarNiCerrarBolsaAjena() throws Exception {
        // Arrange
        var bolsa=crearBolsa(); String id=bolsa.get("bolsaId").asText();
        // Act & Assert
        mvc.perform(get("/api/bolsas/activas/"+comprador.get("id").asText()).header("Authorization","Bearer "+otroCompradorToken))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/bolsas/"+id+"/items").header("Authorization","Bearer "+otroCompradorToken)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/bolsas/"+id+"/cerrar").header("Authorization","Bearer "+otroCompradorToken)).andExpect(status().isForbidden());
        em.flush();
        assertEquals(false,jdbc.queryForObject("select cerrada from bolsas_estilo where id=?",Boolean.class,UUID.fromString(id)));
    }

    @Test void recursosInexistentesDevuelven404ConRolCorrecto() throws Exception {
        // Arrange
        String id=UUID.randomUUID().toString();
        // Act & Assert
        mvc.perform(patch("/api/prendas/"+id+"/suspender").header("Authorization","Bearer "+vendedorToken)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/bolsas/"+id+"/cerrar").header("Authorization","Bearer "+compradorToken)).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings={"/api/prendas","/api/prendas/creador/00000000-0000-0000-0000-000000000001","/api/reportes/creadores/variedad"})
    void consultasSiguenPublicasSinSesion(String path) throws Exception {
        // Arrange: sin cabecera Authorization ni sesión.
        // Act & Assert
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test void swaggerDeclaraBearerYProteccionPorOperacion() throws Exception {
        // Arrange & Act
        var api=body(mvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn());
        // Assert
        assertEquals("bearer",api.at("/components/securitySchemes/bearerAuth/scheme").asText());
        assertEquals("http",api.at("/components/securitySchemes/bearerAuth/type").asText());
        assertEquals("JWT",api.at("/components/securitySchemes/bearerAuth/bearerFormat").asText());
        assertEquals("bearerAuth",api.at("/paths/~1api~1prendas/post/security/0").propertyNames().iterator().next());
        assertTrue(api.at("/paths/~1api~1prendas/get/security").isEmpty());
        assertTrue(api.at("/paths/~1api~1auth~1login/post/security").isEmpty());
        assertTrue(api.at("/paths/~1api~1prendas/post/responses").has("401"));
        assertTrue(api.at("/paths/~1api~1prendas/post/responses").has("403"));
        assertEquals(15,api.get("paths").properties().stream().mapToInt(e -> e.getValue().size()).sum());
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }
}
