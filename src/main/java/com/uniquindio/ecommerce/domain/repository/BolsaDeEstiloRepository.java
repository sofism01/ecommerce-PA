package com.uniquindio.ecommerce.domain.repository;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import java.util.Optional;
import java.util.UUID;

public interface BolsaDeEstiloRepository {

    /**
     * Busca una bolsa de estilo por su identificador único de dominio.
     */
    Optional<BolsaDeEstilo> findById(BolsaDeEstiloId id);

    /**
     * Busca una bolsa de estilo activa asociada a un cliente específico.
     */
    Optional<BolsaDeEstilo> findByClienteIdAndCerradaFalse(UUID clienteId);

    /**
     * Guarda o actualiza una bolsa de estilo (tanto si es nueva como si se han agregado líneas).
     */
    BolsaDeEstilo save(BolsaDeEstilo bolsaDeEstilo);
}
