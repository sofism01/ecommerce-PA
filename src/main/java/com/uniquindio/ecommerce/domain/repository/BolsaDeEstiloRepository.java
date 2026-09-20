package com.uniquindio.ecommerce.domain.repository;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import java.util.Optional;
import java.util.UUID;

public interface BolsaDeEstiloRepository {

    /**
     * Recupera una bolsa de estilo por su identificador único de dominio.
     */
    Optional<BolsaDeEstilo> obtenerBolsaPorId(BolsaDeEstiloId id);

    /**
     * Recupera la bolsa de estilo activa asociada a un cliente específico.
     */
    Optional<BolsaDeEstilo> obtenerBolsaActivaPorCliente(UUID clienteId);

    /**
     * Almacena o actualiza una bolsa de estilo (tanto si es nueva como si se han agregado líneas).
     */
    BolsaDeEstilo guardarBolsa(BolsaDeEstilo bolsaDeEstilo);
}
