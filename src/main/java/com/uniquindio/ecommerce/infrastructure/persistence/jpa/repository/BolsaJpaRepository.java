package com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository;

import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.BolsaJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BolsaJpaRepository extends Repository<BolsaJpaEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BolsaJpaEntity b where b.id = :id")
    Optional<BolsaJpaEntity> bloquear(@Param("id") UUID id);
    @EntityGraph(attributePaths = "lineas")
    Optional<BolsaJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "lineas")
    List<BolsaJpaEntity> findByClienteIdAndCerradaFalseOrderByIdAsc(UUID clienteId);

    BolsaJpaEntity save(BolsaJpaEntity entity);
    void delete(BolsaJpaEntity entity);

    @Query(value = "select count(*) from bolsas_estilo where id = :id", nativeQuery = true)
    long contarIncluyendoEliminados(@Param("id") UUID id);
}
