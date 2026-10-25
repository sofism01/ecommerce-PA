package com.uniquindio.ecommerce.infrastructure.rest.mapper;

import com.uniquindio.ecommerce.application.dto.request.RegistrarPrendaRequest;
import com.uniquindio.ecommerce.application.dto.response.PrendaResponse;
import com.uniquindio.ecommerce.application.dto.response.VariantePrendaResponse;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PrendaMapper {
    private final DineroMapper dineroMapper;

    public PrendaDeAutor toDomain(RegistrarPrendaRequest request) {
        var variantes = request.variantes().stream()
                .map(v -> VariantePrenda.crear(UUID.randomUUID(), new SKU(v.sku()), v.tipoTalla(), v.stockDisponible()))
                .toList();
        return PrendaDeAutor.crear(PrendaDeAutorId.random(), request.creadorId(), request.nombre(),
                request.descripcion(), new Dinero(request.precioBase(), request.moneda()), variantes, request.stockTotal());
    }

    public PrendaResponse toResponse(PrendaDeAutor prenda) {
        var variantes = prenda.getVariantes().stream().map(v -> new VariantePrendaResponse(
                v.id(), v.sku().value(), v.tipoTalla(), v.stockDisponible())).toList();
        return new PrendaResponse(prenda.getId().value(), prenda.getCreadorId(), prenda.getNombre(),
                prenda.getDescripcion(), dineroMapper.toResponse(prenda.getPrecioBase()), variantes,
                prenda.getStockTotal(), prenda.isDisponibleParaLaVenta());
    }

    public PrendaDeAutorId prendaId(UUID id) {
        return new PrendaDeAutorId(id);
    }

    public VariantePrendaId varianteId(UUID id) {
        return new VariantePrendaId(id);
    }
}
