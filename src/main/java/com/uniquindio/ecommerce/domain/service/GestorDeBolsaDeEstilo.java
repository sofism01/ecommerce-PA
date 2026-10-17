package com.uniquindio.ecommerce.domain.service;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import java.util.Optional;

public class GestorDeBolsaDeEstilo {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    public GestorDeBolsaDeEstilo(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        this.bolsaDeEstiloRepository = bolsaDeEstiloRepository;
    }

    public Optional<BolsaDeEstilo> obtenerBolsaActiva(BolsaDeEstiloId id) {
        return bolsaDeEstiloRepository.obtenerBolsaPorId(id)
                .filter(BolsaDeEstilo::estaActiva);
    }

    public BolsaDeEstilo guardarBolsa(BolsaDeEstilo bolsaDeEstilo) {
        return bolsaDeEstiloRepository.guardarBolsa(bolsaDeEstilo);
    }
}