package com.uniquindio.ecommerce.infrastructure.rest.mapper;

import com.uniquindio.ecommerce.application.dto.response.*;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.query.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultaPrendasMapper {
    private final PrendaMapper prendas;

    public PaginaPrendasResponse toResponse(Pagina<PrendaDeAutor> pagina) {
        return new PaginaPrendasResponse(pagina.content().stream().map(prendas::toResponse).toList(),
                pagina.totalElements(), pagina.totalPages(), pagina.page(), pagina.size());
    }

    public VariedadCreadorResponse toResponse(VariedadCreador fila) {
        return new VariedadCreadorResponse(fila.creadorId(), fila.cantidadVariantes(), fila.cantidadPrendas());
    }
}
