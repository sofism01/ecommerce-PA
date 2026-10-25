package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;

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
@Table(name = "lineas_bolsa", indexes = {
        @Index(name = "idx_lineas_bolsa", columnList = "bolsa_id"),
        @Index(name = "idx_lineas_variante", columnList = "variante_id")})
@SoftDelete(columnName = "eliminado", strategy = SoftDeleteType.DELETED)
@Check(constraints = "cantidad > 0 and precio_unitario >= 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LineaBolsaJpaEntity {
    // Identidad técnica de la fila: permite conservar líneas eliminadas y volver a seleccionar la variante.
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false, updatable = false)
    private BolsaJpaEntity bolsa;
    // Referencia externa por ID, como en el dominio. No depende de la disponibilidad actual del catálogo.
    @Column(name = "variante_id", nullable = false) private UUID varianteId;
    @Column(nullable = false) private int cantidad;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "monto", column = @Column(name = "precio_unitario", nullable = false, columnDefinition = "decfloat(100000)")),
            @AttributeOverride(name = "escala", column = @Column(name = "precio_escala", nullable = false)),
            @AttributeOverride(name = "moneda", column = @Column(name = "moneda", nullable = false, columnDefinition = "character varying"))})
    private DineroJpa precioUnitario;
    @Column(nullable = false) private int posicion;

    public LineaBolsaJpaEntity(BolsaJpaEntity bolsa, UUID varianteId) {
        this.id = UUID.randomUUID();
        this.bolsa = bolsa;
        this.varianteId = varianteId;
    }
}
