package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.CodificadorContrasenas;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.exception.CorreoDuplicadoException;
import com.uniquindio.ecommerce.domain.repository.UsuarioRepository;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {
    private final UsuarioRepository usuarios;
    private final CodificadorContrasenas passwords;
    @Transactional
    public Usuario ejecutar(String nombre, String correo, String password, Rol rol) {
        String email = Usuario.normalizarCorreo(correo);
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Contraseña inválida.");
        if (usuarios.porCorreo(email).isPresent()) throw new CorreoDuplicadoException();
        return usuarios.guardar(Usuario.reconstruir(UUID.randomUUID(), nombre, email,
                passwords.codificar(password), rol, true));
    }
}
