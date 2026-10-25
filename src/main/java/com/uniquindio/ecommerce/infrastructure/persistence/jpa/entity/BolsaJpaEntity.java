package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bolsas_estilo", indexes = @Index(name = "idx_bolsas_cliente_estado", columnList = "cliente_id,cerrada"))
@SoftDelete(columnName = "eliminado", strategy = SoftDeleteType.DELETED)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BolsaJpaEntity {
    @Id private UUID id;
    @Version private Long version;
    @Column(name = "cliente_id", nullable = false) private UUID clienteId;
    @Column(nullable = false) private boolean cerrada;
    @OneToMany(mappedBy = "bolsa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Setter(AccessLevel.NONE)
    private List<LineaBolsaJpaEntity> lineas = new ArrayList<>();

    public BolsaJpaEntity(UUID id) {
        this.id = id;
    }
}
