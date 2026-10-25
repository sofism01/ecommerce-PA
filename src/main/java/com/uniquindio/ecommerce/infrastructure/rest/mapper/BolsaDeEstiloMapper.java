package com.uniquindio.ecommerce.infrastructure.rest.mapper;

import com.uniquindio.ecommerce.application.dto.response.BolsaDeEstiloResponse;
import com.uniquindio.ecommerce.application.dto.response.LineaDeBolsaResponse;
import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BolsaDeEstiloMapper {
    private final DineroMapper dineroMapper;

    public BolsaDeEstiloResponse toResponse(BolsaDeEstilo bolsa) {
        var lineas = bolsa.getLineas().stream().map(linea -> new LineaDeBolsaResponse(
                linea.varianteId(), linea.cantidad(), dineroMapper.toResponse(linea.precioUnitario()),
                dineroMapper.toResponse(linea.calcularSubtotal()))).toList();
        return new BolsaDeEstiloResponse(bolsa.getId().valor(), bolsa.getClienteId(), lineas, bolsa.isCerrada());
    }

    public BolsaDeEstiloId bolsaId(UUID id) {
        return new BolsaDeEstiloId(id);
    }
}
