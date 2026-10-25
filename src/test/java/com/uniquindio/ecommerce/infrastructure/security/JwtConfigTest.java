package com.uniquindio.ecommerce.infrastructure.security;

import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {
    @ParameterizedTest @ValueSource(strings={"", "corta", "aW5zZWd1cmE=", "no es base64"})
    void rechazaSecretoAusenteCortoOMalCodificado(String secreto) {
        // Arrange
        var config=new JwtConfig();
        // Act & Assert
        assertThrows(IllegalStateException.class,() -> config.jwtKey(secreto));
    }
    @ParameterizedTest @ValueSource(strings={"PT0S","PT-1S","PT25H","PT0.5S"})
    void rechazaDuracionNoAdmitida(String duracion) {
        // Arrange
        var config=new JwtConfig();
        // Act & Assert
        assertThrows(IllegalStateException.class,() -> config.jwtDuration(Duration.parse(duracion)));
    }
    @Test void dominioNoAceptaPasswordPlanoComoHash() {
        // Arrange: el dominio sólo admite el valor codificado.
        // Act & Assert
        assertThrows(IllegalArgumentException.class,() -> Usuario.reconstruir(UUID.randomUUID(),"Nombre",
                "usuario@boheme.test","PasswordPlano2026!",Rol.COMPRADOR,true));
    }
}
