package com.uniquindio.ecommerce.infrastructure.persistence;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPrendaDeAutorRepository implements PrendaDeAutorRepository {

    private final Map<PrendaDeAutorId, PrendaDeAutor> prendas = new HashMap<>();

    @Override
    public Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id) {
        return Optional.ofNullable(prendas.get(id));
    }

    @Override
    public Optional<PrendaDeAutor> obtenerPrendaPorCreador(UUID creadorId) {
        return prendas.values().stream()
                .filter(prenda -> prenda.getCreadorId().equals(creadorId))
                .findFirst();
    }

    @Override
    public PrendaDeAutor guardarPrenda(PrendaDeAutor prendaDeAutor) {
        prendas.put(prendaDeAutor.getId(), prendaDeAutor);
        return prendaDeAutor;
    }
}
