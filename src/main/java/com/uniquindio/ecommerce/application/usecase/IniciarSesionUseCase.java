package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.*;
import com.uniquindio.ecommerce.application.dto.response.LoginResponse;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class IniciarSesionUseCase {
    private final UsuarioRepository usuarios;
    private final CodificadorContrasenas passwords;
    private final EmisorTokens tokens;
    @Transactional(readOnly=true)
    public LoginResponse ejecutar(String correo, String password) {
        var usuario = usuarios.porCorreo(Usuario.normalizarCorreo(correo));
        boolean correcta = passwords.verificar(password, usuario.map(Usuario::getPasswordHash).orElse(null));
        if (!correcta || usuario.isEmpty() || !usuario.get().isActivo()) throw new CredencialesInvalidasException();
        return tokens.emitir(usuario.get());
    }
}
