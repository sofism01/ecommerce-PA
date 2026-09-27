package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class RegistrarPrendaDeAutorUseCase {

    private final PrendaDeAutorRepository prendaDeAutorRepository;

    public RegistrarPrendaDeAutorUseCase(PrendaDeAutorRepository prendaDeAutorRepository) {
        this.prendaDeAutorRepository = prendaDeAutorRepository;
    }

    // registra una nueva prenda de autor en el sistema
    public PrendaDeAutor ejecutar(UUID creadorId, String nombre, String descripcion, BigDecimal precioBase, String moneda, List<VariantePrenda> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos una variante de talla.");
        }

        Dinero precio = new Dinero(precioBase, moneda);
        PrendaDeAutor nuevaPrenda = PrendaDeAutor.crear(PrendaDeAutorId.random(), creadorId, nombre, descripcion, precio, variantes);

        return prendaDeAutorRepository.guardarPrenda(nuevaPrenda);
    }
}
