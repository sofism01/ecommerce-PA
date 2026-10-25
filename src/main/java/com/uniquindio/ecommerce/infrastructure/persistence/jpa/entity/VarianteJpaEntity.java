package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;

import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import java.util.UUID;

@Entity
@Table(name = "variantes_prenda", indexes = @Index(name = "idx_variantes_prenda", columnList = "prenda_id"))
@SoftDelete(columnName = "eliminado", strategy = SoftDeleteType.DELETED)
@Check(constraints = "stock_disponible >= 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VarianteJpaEntity {
    @Id private UUID id;
    // El padre usa @SoftDelete; EAGER evita proxies a una fila lógicamente eliminada.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false, updatable = false)
    private PrendaJpaEntity prenda;
    @Column(nullable = false, columnDefinition = "character varying") private String sku;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_talla", nullable = false) private TipoTalla tipoTalla;
    @Column(name = "stock_disponible", nullable = false) private int stockDisponible;
    @Column(nullable = false) private int posicion;

    public VarianteJpaEntity(UUID id, PrendaJpaEntity prenda) {
        this.id = id;
        this.prenda = prenda;
    }
}
