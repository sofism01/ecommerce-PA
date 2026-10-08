package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;

public class RegistrarPrendaDeAutorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    public RegistrarPrendaDeAutorUseCase(PrendaDeAutorRepository prendaDeAutorRepository) {
        this.prendaDeAutorRepository = prendaDeAutorRepository;
    }

    // registra una nueva prenda de autor en el sistema
    public PrendaDeAutor ejecutar(PrendaDeAutor prendaDeAutor) {
        return prendaDeAutorRepository.guardarPrenda(prendaDeAutor);
    }
}
