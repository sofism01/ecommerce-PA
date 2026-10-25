package com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper;

import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.DineroJpa;
import org.springframework.stereotype.Component;

@Component
public class DineroJpaMapper {
    public DineroJpa toJpa(Dinero dinero) {
        return new DineroJpa(dinero.monto(), dinero.monto().scale(), dinero.moneda());
    }

    public Dinero toDomain(DineroJpa dinero) {
        return new Dinero(dinero.getMonto().setScale(dinero.getEscala()), dinero.getMoneda());
    }
}
