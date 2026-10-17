package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObtenerBolsaDeEstiloActivaUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    @Transactional(readOnly = true)
    public Optional<BolsaDeEstilo> ejecutar(UUID clienteId) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo.");
        }

        return bolsaDeEstiloRepository.obtenerBolsaActivaPorCliente(clienteId);
    }
}