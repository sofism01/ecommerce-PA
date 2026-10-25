package com.uniquindio.ecommerce.domain.service;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

import java.util.Optional;

public class ActualizadorDeStock {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    public ActualizadorDeStock(PrendaDeAutorRepository prendaDeAutorRepository) {
        this.prendaDeAutorRepository = prendaDeAutorRepository;
    }

    public void actualizarStock(PrendaDeAutorId id, int nuevoStock) {
        Optional<PrendaDeAutor> prenda = prendaDeAutorRepository.obtenerPrendaPorId(id);
        prenda.ifPresent(p -> {
            p.actualizarStock(nuevoStock);
            prendaDeAutorRepository.guardarPrenda(p);
        });
    }
}