package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prendas_autor", indexes = {
        @Index(name = "idx_prendas_creador", columnList = "creador_id"),
        @Index(name = "idx_prendas_precio", columnList = "precio_base")})
@SoftDelete(columnName = "eliminado", strategy = SoftDeleteType.DELETED)
@Check(constraints = "stock_total >= 0 and precio_base > 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrendaJpaEntity {
    @Id private UUID id;
    @Version private Long version;
    @Column(name = "creador_id", nullable = false) private UUID creadorId;
    @Column(nullable = false, columnDefinition = "character varying") private String nombre;
    @Column(nullable = false, columnDefinition = "character varying") private String descripcion;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "monto", column = @Column(name = "precio_base", nullable = false, columnDefinition = "decfloat(100000)")),
            @AttributeOverride(name = "escala", column = @Column(name = "precio_escala", nullable = false)),
            @AttributeOverride(name = "moneda", column = @Column(name = "moneda", nullable = false, columnDefinition = "character varying"))})
    private DineroJpa precio;
    @Column(name = "disponible_para_venta", nullable = false) private boolean disponibleParaLaVenta;
    @Column(name = "stock_total", nullable = false) private int stockTotal;
    @OneToMany(mappedBy = "prenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Setter(AccessLevel.NONE)
    private List<VarianteJpaEntity> variantes = new ArrayList<>();

    public PrendaJpaEntity(UUID id) {
        this.id = id;
    }
}
