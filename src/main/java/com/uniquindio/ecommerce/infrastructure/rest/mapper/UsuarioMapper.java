package com.uniquindio.ecommerce.infrastructure.rest.mapper;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.application.dto.response.UsuarioResponse;
import org.springframework.stereotype.Component;
@Component
public class UsuarioMapper {
    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(),u.getNombre(),u.getCorreo(),u.getRol(),u.isActivo());
    }
}
