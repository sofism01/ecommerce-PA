package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListarPrendasPorCreadorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    @Transactional(readOnly = true)
    public List<PrendaDeAutor> ejecutar(UUID creadorId) {
        if (creadorId == null) {
            throw new IllegalArgumentException("El ID del creador no puede ser nulo.");
        }

        return prendaDeAutorRepository.obtenerPrendasPorCreador(creadorId);
    }
}
