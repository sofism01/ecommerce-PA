package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

public class VaciarBolsaDeEstiloUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    public VaciarBolsaDeEstiloUseCase(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        this.bolsaDeEstiloRepository = bolsaDeEstiloRepository;
    }

    // Vacía todas las líneas de productos de una bolsa de estilo
    public void ejecutar(BolsaDeEstiloId bolsaId) {
        BolsaDeEstilo bolsa = bolsaDeEstiloRepository.obtenerBolsaPorId(bolsaId)
                .orElseThrow(() -> new IllegalArgumentException("La bolsa de estilo no existe."));

        bolsa.vaciar();

        bolsaDeEstiloRepository.guardarBolsa(bolsa);
    }
}
