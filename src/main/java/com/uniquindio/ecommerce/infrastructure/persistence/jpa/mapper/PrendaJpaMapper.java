package com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.domain.valueobject.SKU;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.PrendaJpaEntity;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.VarianteJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.function.Function;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PrendaJpaMapper {
    private final DineroJpaMapper dineroMapper;

    public PrendaJpaEntity toJpa(PrendaDeAutor dominio) {
        var entity = new PrendaJpaEntity(dominio.getId().value());
        actualizar(dominio, entity);
        return entity;
    }

    /** Actualiza instancias gestionadas, conservando versiones e identidades de las variantes. */
    public void actualizar(PrendaDeAutor dominio, PrendaJpaEntity entity) {
        entity.setCreadorId(dominio.getCreadorId());
        entity.setNombre(dominio.getNombre());
        entity.setDescripcion(dominio.getDescripcion());
        entity.setPrecio(dineroMapper.toJpa(dominio.getPrecioBase()));
        entity.setDisponibleParaLaVenta(dominio.isDisponibleParaLaVenta());
        entity.setStockTotal(dominio.getStockTotal());

        var ids = dominio.getVariantes().stream().map(VariantePrenda::id).collect(Collectors.toSet());
        entity.getVariantes().removeIf(v -> !ids.contains(v.getId()));
        var existentes = entity.getVariantes().stream().collect(Collectors.toMap(VarianteJpaEntity::getId, Function.identity()));
        for (int i = 0; i < dominio.getVariantes().size(); i++) {
            var variante = dominio.getVariantes().get(i);
            var fila = existentes.get(variante.id());
            if (fila == null) {
                fila = new VarianteJpaEntity(variante.id(), entity);
                entity.getVariantes().add(fila);
            }
            fila.setSku(variante.sku().value());
            fila.setTipoTalla(variante.tipoTalla());
            fila.setStockDisponible(variante.stockDisponible());
            fila.setPosicion(i);
        }
    }

    public PrendaDeAutor toDomain(PrendaJpaEntity entity) {
        var variantes = entity.getVariantes().stream().sorted(Comparator.comparingInt(VarianteJpaEntity::getPosicion)).map(v -> VariantePrenda.crear(
                v.getId(), new SKU(v.getSku()), v.getTipoTalla(), v.getStockDisponible())).toList();
        return PrendaDeAutor.reconstruir(new PrendaDeAutorId(entity.getId()), entity.getCreadorId(),
                entity.getNombre(), entity.getDescripcion(), dineroMapper.toDomain(entity.getPrecio()),
                variantes, entity.isDisponibleParaLaVenta(), entity.getStockTotal());
    }
}
