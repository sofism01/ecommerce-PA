package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble en memoria para pruebas; no se registra como bean de la aplicación. */
public class InMemoryPrendaDeAutorRepository implements PrendaDeAutorRepository {

    private final Map<PrendaDeAutorId, PrendaDeAutor> prendas = new HashMap<>();

    @Override
    public Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id) {
        return Optional.ofNullable(prendas.get(id));
    }

    @Override
    public List<PrendaDeAutor> obtenerPrendasPorCreador(UUID creadorId) {
        return prendas.values().stream()
                .filter(prenda -> prenda.getCreadorId().equals(creadorId))
                .toList();
    }

    @Override
    public PrendaDeAutor guardarPrenda(PrendaDeAutor prendaDeAutor) {
        prendas.put(prendaDeAutor.getId(), prendaDeAutor);
        return prendaDeAutor;
    }
}
