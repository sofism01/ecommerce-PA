package com.uniquindio.ecommerce.infrastructure.rest.mapper;

import com.uniquindio.ecommerce.application.dto.response.DineroResponse;
import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import org.springframework.stereotype.Component;

@Component
public class DineroMapper {
    public DineroResponse toResponse(Dinero dinero) {
        return new DineroResponse(dinero.monto(), dinero.moneda());
    }
}
