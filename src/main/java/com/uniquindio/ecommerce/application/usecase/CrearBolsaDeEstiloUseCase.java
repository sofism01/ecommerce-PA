package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrearBolsaDeEstiloUseCase {

    private final BolsaDeEstiloRepository bolsaDeEstiloRepository;

    @Transactional
    public BolsaDeEstilo ejecutar(UUID clienteId) {
        BolsaDeEstilo nuevaBolsa = BolsaDeEstilo.crear(new BolsaDeEstiloId(UUID.randomUUID()), clienteId);
        return bolsaDeEstiloRepository.guardarBolsa(nuevaBolsa);
    }
}