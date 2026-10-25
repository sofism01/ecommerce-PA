package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;
import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.util.UUID;
@Entity
@Table(name="lineas_pedido",uniqueConstraints=@UniqueConstraint(name="uk_linea_pedido_variante",columnNames={"pedido_id","variante_id"}))
@Check(constraints="cantidad > 0 and precio_unitario > 0")
@Getter @Setter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class LineaPedidoJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="pedido_id",nullable=false,updatable=false) private PedidoJpaEntity pedido;
    @Column(name="prenda_id",nullable=false,updatable=false) private UUID prendaId;
    @Column(name="variante_id",nullable=false,updatable=false) private UUID varianteId;
    @Column(name="nombre_prenda",nullable=false,updatable=false,columnDefinition="character varying") private String nombrePrenda;
    @Column(nullable=false,updatable=false,columnDefinition="character varying") private String sku;
    @Enumerated(EnumType.STRING) @Column(name="tipo_talla",nullable=false,updatable=false) private TipoTalla tipoTalla;
    @Column(nullable=false,updatable=false) private int cantidad;
    @Column(nullable=false,updatable=false) private int posicion;
    @Embedded @AttributeOverrides({
        @AttributeOverride(name="monto",column=@Column(name="precio_unitario",nullable=false,updatable=false,columnDefinition="decfloat(100000)")),
        @AttributeOverride(name="escala",column=@Column(name="precio_escala",nullable=false,updatable=false)),
        @AttributeOverride(name="moneda",column=@Column(name="moneda",nullable=false,updatable=false))})
    private DineroJpa precioUnitario;
    public LineaPedidoJpaEntity(PedidoJpaEntity pedido) { this.id=UUID.randomUUID(); this.pedido=pedido; }
}
