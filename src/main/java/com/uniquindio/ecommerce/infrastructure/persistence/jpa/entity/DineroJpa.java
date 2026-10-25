package com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DineroJpa {
    // H2: decimal de escala variable; la escala original se conserva explícitamente.
    @Column(nullable = false, columnDefinition = "decfloat(100000)")
    private BigDecimal monto;
    @Column(nullable = false)
    private int escala;
    @Column(nullable = false, columnDefinition = "character varying")
    private String moneda;
}
