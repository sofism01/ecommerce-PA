package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

public class CerrarBolsaDeEstiloUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    public CerrarBolsaDeEstiloUseCase(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        this.bolsaDeEstiloRepository = bolsaDeEstiloRepository;
    }

    // Cambia el estado de una bolsa de estilo a cerrada
    public void ejecutar(BolsaDeEstiloId bolsaId) {
        BolsaDeEstilo bolsa = bolsaDeEstiloRepository.obtenerBolsaPorId(bolsaId)
                .orElseThrow(() -> new IllegalArgumentException("La bolsa de estilo no existe."));

        bolsa.cerrarBolsa();

        bolsaDeEstiloRepository.guardarBolsa(bolsa);
    }
}
