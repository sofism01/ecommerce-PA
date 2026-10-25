package com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.UsuarioJpaEntity;
import org.springframework.stereotype.Component;
@Component
public class UsuarioJpaMapper {
    public Usuario toDomain(UsuarioJpaEntity e) {
        return Usuario.reconstruir(e.getId(),e.getNombre(),e.getCorreo(),e.getPasswordHash(),e.getRol(),e.isActivo());
    }
    public UsuarioJpaEntity toJpa(Usuario u) {
        var e=new UsuarioJpaEntity(u.getId());
        e.setNombre(u.getNombre()); e.setCorreo(u.getCorreo()); e.setPasswordHash(u.getPasswordHash());
        e.setRol(u.getRol()); e.setActivo(u.isActivo());
        return e;
    }
}
