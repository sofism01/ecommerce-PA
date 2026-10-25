package com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import com.uniquindio.ecommerce.domain.valueobject.LineaDeBolsa;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.BolsaJpaEntity;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.entity.LineaBolsaJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.function.Function;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BolsaJpaMapper {
    private final DineroJpaMapper dineroMapper;

    public BolsaJpaEntity toJpa(BolsaDeEstilo dominio) {
        var entity = new BolsaJpaEntity(dominio.getId().valor());
        actualizar(dominio, entity);
        return entity;
    }

    public void actualizar(BolsaDeEstilo dominio, BolsaJpaEntity entity) {
        entity.setClienteId(dominio.getClienteId());
        entity.setCerrada(dominio.isCerrada());
        var ids = dominio.getLineas().stream().map(LineaDeBolsa::varianteId).collect(Collectors.toSet());
        entity.getLineas().removeIf(linea -> !ids.contains(linea.getVarianteId()));
        var existentes = entity.getLineas().stream().collect(Collectors.toMap(LineaBolsaJpaEntity::getVarianteId, Function.identity()));
        for (int i = 0; i < dominio.getLineas().size(); i++) {
            var linea = dominio.getLineas().get(i);
            var fila = existentes.get(linea.varianteId());
            if (fila == null) {
                fila = new LineaBolsaJpaEntity(entity, linea.varianteId());
                entity.getLineas().add(fila);
            }
            fila.setCantidad(linea.cantidad());
            fila.setPrecioUnitario(dineroMapper.toJpa(linea.precioUnitario()));
            fila.setPosicion(i);
        }
    }

    public BolsaDeEstilo toDomain(BolsaJpaEntity entity) {
        var lineas = entity.getLineas().stream().sorted(Comparator.comparingInt(LineaBolsaJpaEntity::getPosicion)).map(linea -> LineaDeBolsa.crear(
                linea.getVarianteId(), linea.getCantidad(), dineroMapper.toDomain(linea.getPrecioUnitario()))).toList();
        return BolsaDeEstilo.reconstruir(new BolsaDeEstiloId(entity.getId()), entity.getClienteId(), lineas, entity.isCerrada());
    }
}
