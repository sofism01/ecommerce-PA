package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.domain.valueobject.VariantePrendaId;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActualizarStockVariantePrendaUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    @Transactional
    public void ejecutar(PrendaDeAutorId prendaId, VariantePrendaId varianteId, int nuevoStock) {
        PrendaDeAutor prenda = prendaDeAutorRepository.obtenerPrendaPorId(prendaId)
                .orElseThrow(() -> new IllegalArgumentException("La prenda de autor no existe."));

        prenda.actualizarStockVariante(varianteId, nuevoStock);

        prendaDeAutorRepository.guardarPrenda(prenda);
    }
}