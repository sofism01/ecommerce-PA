package com.uniquindio.ecommerce.infrastructure.persistence.jpa;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.domain.repository.UsuarioRepository;
import com.uniquindio.ecommerce.domain.exception.CorreoDuplicadoException;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.UsuarioJpaRepository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.UsuarioJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Repository
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class JpaUsuarioRepository implements UsuarioRepository {
    private final UsuarioJpaRepository repository;
    private final UsuarioJpaMapper mapper;
    public Optional<Usuario> porCorreo(String correo) { return repository.findByCorreo(correo).map(mapper::toDomain); }
    public Optional<Usuario> porId(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Transactional
    public Usuario guardar(Usuario usuario) {
        try { return mapper.toDomain(repository.saveAndFlush(mapper.toJpa(usuario))); }
        catch (DataIntegrityViolationException e) {
            // El único índice único adicional al UUID es el correo normalizado.
            if (e.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT).contains("uk_usuario_correo"))
                throw new CorreoDuplicadoException();
            throw e;
        }
    }
}
