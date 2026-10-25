package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.exception.CorreoDuplicadoException;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.UsuarioJpaMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql=false)
@Import({JpaUsuarioRepository.class,UsuarioJpaMapper.class})
class UsuarioJpaTest {
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired EntityManager em;
    private Usuario usuario(String correo,Rol rol,boolean activo) {
        return Usuario.reconstruir(UUID.randomUUID(),"Usuario",correo,
                new BCryptPasswordEncoder(4).encode("PruebaSegura2026!"),rol,activo);
    }
    @Test void persisteYReconstruyeTodosLosCamposSinAcoplarDominioAJpa() {
        // Arrange
        var original=usuario("VENDEDOR@BOHEME.TEST",Rol.VENDEDOR,true);
        // Act
        usuarios.guardar(original); em.flush(); em.clear();
        var leido=usuarios.porCorreo("vendedor@boheme.test").orElseThrow();
        // Assert
        assertEquals(original.getId(),leido.getId());
        assertEquals(original.getNombre(),leido.getNombre());
        assertEquals(original.getPasswordHash(),leido.getPasswordHash());
        assertEquals(Rol.VENDEDOR,leido.getRol()); assertTrue(leido.isActivo());
        assertTrue(usuarios.porId(original.getId()).isPresent());
    }
    @Test void indiceUnicoImpideDuplicadosAunqueSeOmitaConsultaPrevia() {
        // Arrange
        usuarios.guardar(usuario("unico@boheme.test",Rol.COMPRADOR,true)); em.flush(); em.clear();
        var repetido=usuario("UNICO@BOHEME.TEST",Rol.VENDEDOR,true);
        // Act & Assert
        assertThrows(CorreoDuplicadoException.class,() -> usuarios.guardar(repetido));
    }
    @Test void conservaCuentaInactivaParaDenegarAutenticacion() {
        // Arrange
        var original=usuario("inactivo@boheme.test",Rol.COMPRADOR,false);
        // Act
        usuarios.guardar(original); em.flush(); em.clear();
        // Assert
        assertFalse(usuarios.porId(original.getId()).orElseThrow().isActivo());
    }
}
