package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

public class SuspenderVentaPrendaDeAutorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    public SuspenderVentaPrendaDeAutorUseCase(PrendaDeAutorRepository prendaDeAutorRepository) {
        this.prendaDeAutorRepository = prendaDeAutorRepository;
    }

    // Suspende temporalmente la venta de una prenda de autor
    public void ejecutar(PrendaDeAutorId prendaId) {
        PrendaDeAutor prenda = prendaDeAutorRepository.obtenerPrendaPorId(prendaId)
                .orElseThrow(() -> new IllegalArgumentException("La prenda de autor no existe."));

        prenda.suspenderVenta();

        prendaDeAutorRepository.guardarPrenda(prenda);
    }
}
