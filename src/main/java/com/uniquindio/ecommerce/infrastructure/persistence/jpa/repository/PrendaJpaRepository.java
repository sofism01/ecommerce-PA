package com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository;

import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.PrendaJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.uniquindio.ecommerce.domain.query.VariedadCreador;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Superficie de persistencia acotada: no expone borrados físicos ni operaciones batch. */
public interface PrendaJpaRepository extends Repository<PrendaJpaEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrendaJpaEntity p where p.id = :id")
    Optional<PrendaJpaEntity> bloquear(@Param("id") UUID id);

    @Query("select p.id from PrendaJpaEntity p join p.variantes v where v.id = :varianteId")
    Optional<UUID> prendaDeVariante(@Param("varianteId") UUID varianteId);
    // Sin fetch de colección en la consulta paginada: LIMIT/OFFSET se ejecutan en SQL.
    @Query(value = """
            select p.id from PrendaJpaEntity p
            where (:texto is null or lower(p.nombre) like :texto escape '!'
                or lower(p.descripcion) like :texto escape '!')
              and (:creadorId is null or p.creadorId = :creadorId)
              and (:moneda is null or upper(p.precio.moneda) = :moneda)
            order by p.nombre asc, p.id asc
            """, countQuery = """
            select count(p.id) from PrendaJpaEntity p
            where (:texto is null or lower(p.nombre) like :texto escape '!'
                or lower(p.descripcion) like :texto escape '!')
              and (:creadorId is null or p.creadorId = :creadorId)
              and (:moneda is null or upper(p.precio.moneda) = :moneda)
            """)
    Page<UUID> buscarIds(@Param("texto") String texto, @Param("creadorId") UUID creadorId,
                        @Param("moneda") String moneda, Pageable pageable);

    @Query("""
            select distinct p from PrendaJpaEntity p
            left join fetch p.variantes
            where p.id in :ids
            order by p.nombre asc, p.id asc
            """)
    List<PrendaJpaEntity> cargarPaginaConVariantes(@Param("ids") List<UUID> ids);

    // @SoftDelete añade los predicados de prendas y variantes al SQL generado.
    @Query("""
            select new com.uniquindio.ecommerce.domain.query.VariedadCreador(
                p.creadorId, count(v.id), count(distinct p.id))
            from PrendaJpaEntity p join p.variantes v
            where p.disponibleParaLaVenta = true
            group by p.creadorId
            order by count(v.id) desc, p.creadorId asc
            """)
    List<VariedadCreador> rankingVariedad();

    @EntityGraph(attributePaths = "variantes")
    Optional<PrendaJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "variantes")
    List<PrendaJpaEntity> findByCreadorIdOrderByIdAsc(UUID creadorId);

    PrendaJpaEntity save(PrendaJpaEntity entity);

    // Hibernate @SoftDelete traduce esta operación en UPDATE, incluyendo las cascadas.
    void delete(PrendaJpaEntity entity);

    @Query(value = "select count(*) from prendas_autor where id = :id", nativeQuery = true)
    long contarIncluyendoEliminados(@Param("id") UUID id);
}
