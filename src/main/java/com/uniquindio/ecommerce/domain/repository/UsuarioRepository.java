package com.uniquindio.ecommerce.domain.repository;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import java.util.Optional;
import java.util.UUID;
public interface UsuarioRepository {
    Optional<Usuario> porCorreo(String correo);
    Optional<Usuario> porId(UUID id);
    Usuario guardar(Usuario usuario);
}
