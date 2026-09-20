package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import java.util.UUID;

public class CrearBolsaDeEstiloUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    public CrearBolsaDeEstiloUseCase(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        this.bolsaDeEstiloRepository = bolsaDeEstiloRepository;
    }

    public BolsaDeEstilo ejecutar(UUID clienteId) {
        BolsaDeEstilo nuevaBolsa = new BolsaDeEstilo(new BolsaDeEstiloId(UUID.randomUUID()), clienteId);
        return bolsaDeEstiloRepository.guardarBolsa(nuevaBolsa);
    }
}
