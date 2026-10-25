package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;
import com.uniquindio.ecommerce.domain.valueobject.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;
@Entity
@Table(name="pedidos_moda",uniqueConstraints=@UniqueConstraint(name="uk_pedido_bolsa",columnNames="bolsa_id"))
@Getter @Setter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PedidoJpaEntity {
    @Id private UUID id;
    @Version private Long version;
    @Column(name="bolsa_id",nullable=false,updatable=false) private UUID bolsaId;
    @Column(name="cliente_id",nullable=false,updatable=false) private UUID clienteId;
    @Column(name="fecha_creacion",nullable=false,updatable=false,columnDefinition="timestamp(9)") private LocalDateTime fechaCreacion;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private EstadoPedido estado;
    @Embedded @AttributeOverrides({
        @AttributeOverride(name="monto",column=@Column(name="total",nullable=false,columnDefinition="decfloat(100000)")),
        @AttributeOverride(name="escala",column=@Column(name="total_escala",nullable=false)),
        @AttributeOverride(name="moneda",column=@Column(name="moneda",nullable=false))})
    private DineroJpa total;
    @OneToMany(mappedBy="pedido",cascade=CascadeType.PERSIST)
    @OrderBy("posicion ASC")
    @Setter(AccessLevel.NONE)
    private List<LineaPedidoJpaEntity> lineas=new ArrayList<>();
    public PedidoJpaEntity(UUID id) { this.id=id; }
}
