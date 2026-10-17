package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;

import java.util.Optional;
import java.util.UUID;

public class ObtenerBolsaDeEstiloActivaUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    public ObtenerBolsaDeEstiloActivaUseCase(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        this.bolsaDeEstiloRepository = bolsaDeEstiloRepository;
    }

    // Obtiene la bolsa de estilo activa de un cliente
    public Optional<BolsaDeEstilo> ejecutar(UUID clienteId) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo.");
        }

        return bolsaDeEstiloRepository.obtenerBolsaActivaPorCliente(clienteId);
    }
}