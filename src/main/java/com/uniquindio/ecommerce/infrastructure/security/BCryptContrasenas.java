package com.uniquindio.ecommerce.infrastructure.security;
import com.uniquindio.ecommerce.application.security.CodificadorContrasenas;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class BCryptContrasenas implements CodificadorContrasenas {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    // Mismo trabajo BCrypt para correo inexistente; no se guarda ni se usa como credencial.
    private final String dummy = encoder.encode(java.util.UUID.randomUUID().toString());
    public String codificar(String password) { return encoder.encode(password); }
    public boolean verificar(String password, String hash) { return encoder.matches(password, hash == null ? dummy : hash); }
}
