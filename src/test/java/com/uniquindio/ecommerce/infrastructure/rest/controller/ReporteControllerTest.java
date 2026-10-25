package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.usecase.ConsultarVariedadCreadoresUseCase;
import com.uniquindio.ecommerce.domain.query.VariedadCreador;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteController.class)
@Import({com.uniquindio.ecommerce.infrastructure.security.SecurityConfig.class, ConsultaPrendasMapper.class, PrendaMapper.class, DineroMapper.class})
class ReporteControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean com.uniquindio.ecommerce.domain.repository.UsuarioRepository usuarios;
    @MockitoBean ConsultarVariedadCreadoresUseCase consultar;

    @Test
    void variedad_Devuelve200ConConteosYDelega() throws Exception {
        // Arrange
        var creador = new UUID(0, 1);
        when(consultar.ejecutar()).thenReturn(List.of(new VariedadCreador(creador, 4, 2)));
        // Act & Assert
        mvc.perform(get("/api/reportes/creadores/variedad")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creadorId").value(creador.toString()))
                .andExpect(jsonPath("$[0].cantidadVariantes").value(4))
                .andExpect(jsonPath("$[0].cantidadPrendas").value(2));
        verify(consultar).ejecutar();
    }

    @Test
    void variedad_SinDatosDevuelve200YArrayVacio() throws Exception {
        // Arrange
        when(consultar.ejecutar()).thenReturn(List.of());
        // Act & Assert
        mvc.perform(get("/api/reportes/creadores/variedad")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
