package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarPrendaDeAutorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    @Transactional
    public PrendaDeAutor ejecutar(PrendaDeAutor prendaDeAutor) {
        return prendaDeAutorRepository.guardarPrenda(prendaDeAutor);
    }
}
