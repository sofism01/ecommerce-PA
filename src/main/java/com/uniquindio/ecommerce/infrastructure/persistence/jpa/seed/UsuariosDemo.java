package com.uniquindio.ecommerce.infrastructure.persistence.jpa.seed;
import com.uniquindio.ecommerce.application.security.CodificadorContrasenas;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.repository.UsuarioRepository;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Component @Profile("dev")
@ConditionalOnProperty(name="boheme.usuarios-demo.enabled",havingValue="true")
@RequiredArgsConstructor
public class UsuariosDemo implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final CodificadorContrasenas passwords;
    @Value("${boheme.demo.password}") private String password;
    @Transactional
    public void run(String... args) {
        crear(DatosIniciales.CREADOR_UNO,"vendedor1@boheme.test",Rol.VENDEDOR);
        crear(DatosIniciales.CREADOR_DOS,"vendedor2@boheme.test",Rol.VENDEDOR);
        crear(DatosIniciales.CLIENTE_UNO,"comprador1@boheme.test",Rol.COMPRADOR);
        crear(DatosIniciales.CLIENTE_DOS,"comprador2@boheme.test",Rol.COMPRADOR);
    }
    private void crear(UUID id,String correo,Rol rol) {
        if (usuarios.porId(id).isEmpty() && usuarios.porCorreo(correo).isEmpty())
            usuarios.guardar(Usuario.reconstruir(id,"Cuenta demo "+rol,correo,passwords.codificar(password),rol,true));
    }
}
