package com.uniquindio.ecommerce.infrastructure.rest.mapper;
import com.uniquindio.ecommerce.domain.entity.PedidoDeModa;
import com.uniquindio.ecommerce.application.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PedidoMapper {
    private final DineroMapper dinero;
    public PedidoResponse toResponse(PedidoDeModa p) {
        var lineas=p.getLineas().stream().map(l -> new LineaPedidoResponse(l.prendaId(),l.varianteId(),l.nombrePrenda(),
                l.sku().value(),l.tipoTalla(),l.cantidad(),dinero.toResponse(l.precioUnitario()),dinero.toResponse(l.calcularSubtotal()))).toList();
        var total=p.calcularTotal();
        return new PedidoResponse(p.getId(),p.getBolsaId(),p.getClienteId(),p.getEstado(),p.getFechaCreacion(),
                lineas,dinero.toResponse(total),total.moneda());
    }
}
