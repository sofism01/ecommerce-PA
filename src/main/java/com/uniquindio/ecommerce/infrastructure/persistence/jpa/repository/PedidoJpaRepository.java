package com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.PedidoJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;
import java.util.*;
/** Sin operaciones de borrado o actualización masiva: conserva el histórico. */
public interface PedidoJpaRepository extends Repository<PedidoJpaEntity,UUID> {
    PedidoJpaEntity save(PedidoJpaEntity pedido);
    @EntityGraph(attributePaths="lineas")
    Optional<PedidoJpaEntity> findById(UUID id);
    void flush();
}
