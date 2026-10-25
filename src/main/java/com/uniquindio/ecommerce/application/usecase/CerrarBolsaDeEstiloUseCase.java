package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.exception.BolsaDeEstiloNoEncontradaException;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CerrarBolsaDeEstiloUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    @Transactional
    public void ejecutar(BolsaDeEstiloId bolsaId) {
        BolsaDeEstilo bolsa = bolsaDeEstiloRepository.obtenerBolsaParaActualizar(bolsaId)
                .orElseThrow(() -> new BolsaDeEstiloNoEncontradaException("La bolsa de estilo no existe."));

        bolsa.cerrarBolsa();

        bolsaDeEstiloRepository.guardarBolsa(bolsa);
    }
}
