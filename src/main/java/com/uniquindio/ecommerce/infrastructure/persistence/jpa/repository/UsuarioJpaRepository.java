package com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.UsuarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID> {
    Optional<UsuarioJpaEntity> findByCorreo(String correo);
}
