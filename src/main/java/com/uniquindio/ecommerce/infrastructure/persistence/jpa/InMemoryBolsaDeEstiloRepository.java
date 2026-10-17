package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryBolsaDeEstiloRepository implements BolsaDeEstiloRepository {

    private final Map<BolsaDeEstiloId, BolsaDeEstilo> bolsas = new HashMap<>();

    @Override
    public Optional<BolsaDeEstilo> obtenerBolsaPorId(BolsaDeEstiloId id) {
        return Optional.ofNullable(bolsas.get(id));
    }

    @Override
    public Optional<BolsaDeEstilo> obtenerBolsaActivaPorCliente(UUID clienteId) {
        return bolsas.values().stream()
                .filter(bolsa -> bolsa.getClienteId().equals(clienteId) && bolsa.estaActiva())
                .findFirst();
    }

    @Override
    public BolsaDeEstilo guardarBolsa(BolsaDeEstilo bolsaDeEstilo) {
        bolsas.put(bolsaDeEstilo.getId(), bolsaDeEstilo);
        return bolsaDeEstilo;
    }
}