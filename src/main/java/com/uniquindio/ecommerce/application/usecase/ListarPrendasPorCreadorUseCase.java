package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListarPrendasPorCreadorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    public ListarPrendasPorCreadorUseCase(PrendaDeAutorRepository prendaDeAutorRepository) {
        this.prendaDeAutorRepository = prendaDeAutorRepository;
    }

    // Obtiene todas las prendas asociadas a un creador específico
    public Optional<PrendaDeAutor> ejecutar(UUID creadorId) {
        if (creadorId == null) {
            throw new IllegalArgumentException("El ID del creador no puede ser nulo.");
        }

        return prendaDeAutorRepository.obtenerPrendaPorCreador(creadorId);
    }
}
