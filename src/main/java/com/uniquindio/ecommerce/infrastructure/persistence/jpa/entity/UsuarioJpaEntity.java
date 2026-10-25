package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;
import com.uniquindio.ecommerce.domain.valueobject.Rol;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity
@Table(name="usuarios", uniqueConstraints=@UniqueConstraint(name="uk_usuario_correo", columnNames="correo"))
@Getter @Setter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class UsuarioJpaEntity {
    @Id private UUID id;
    @Column(nullable=false, length=100) private String nombre;
    @Column(nullable=false, length=254) private String correo;
    @Column(name="password_hash", nullable=false, length=60) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) private Rol rol;
    @Column(nullable=false) private boolean activo;
    public UsuarioJpaEntity(UUID id) { this.id=id; }
}
