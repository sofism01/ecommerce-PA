package com.uniquindio.ecommerce.application.dto.request;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
public record RegistroRequest(
        @NotBlank @Size(max=100) String nombre,
        @NotBlank @Email @Size(max=254) @Pattern(regexp="^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") String correo,
        @NotBlank @Size(min=12, max=72) String password,
        @NotNull Rol rol) {
    @AssertTrue(message="La contraseña no puede superar 72 bytes UTF-8.")
    @io.swagger.v3.oas.annotations.media.Schema(hidden=true)
    public boolean isPasswordBytesValidos() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
