package com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper;
import com.uniquindio.ecommerce.domain.entity.PedidoDeModa;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PedidoJpaMapper {
    private final DineroJpaMapper dinero;
    public PedidoJpaEntity toJpa(PedidoDeModa p) {
        if(p.getBolsaId()==null || p.getLineas().isEmpty())
            throw new ReglaDominioException("Sólo se persisten pedidos con líneas históricas y bolsa de origen.");
        var e=new PedidoJpaEntity(p.getId());
        e.setClienteId(p.getClienteId()); e.setBolsaId(p.getBolsaId());
        e.setEstado(p.getEstado()); e.setFechaCreacion(p.getFechaCreacion()); e.setTotal(dinero.toJpa(p.calcularTotal()));
        for(int i=0;i<p.getLineas().size();i++) {
            var l=p.getLineas().get(i); var fila=new LineaPedidoJpaEntity(e);
            fila.setPrendaId(l.prendaId()); fila.setVarianteId(l.varianteId()); fila.setNombrePrenda(l.nombrePrenda());
            fila.setSku(l.sku().value()); fila.setTipoTalla(l.tipoTalla()); fila.setCantidad(l.cantidad());
            fila.setPrecioUnitario(dinero.toJpa(l.precioUnitario())); fila.setPosicion(i); e.getLineas().add(fila);
        }
        return e;
    }
    public PedidoDeModa toDomain(PedidoJpaEntity e) {
        var lineas=e.getLineas().stream().map(l -> new LineaDePedido(l.getPrendaId(),l.getVarianteId(),l.getNombrePrenda(),
                new SKU(l.getSku()),l.getTipoTalla(),l.getCantidad(),dinero.toDomain(l.getPrecioUnitario()))).toList();
        var p=PedidoDeModa.reconstruir(e.getId(),e.getClienteId(),e.getBolsaId(),lineas,e.getEstado(),e.getFechaCreacion());
        if(!p.calcularTotal().equals(dinero.toDomain(e.getTotal())))
            throw new ReglaDominioException("El total persistido del pedido no coincide con sus líneas.");
        return p;
    }
}
