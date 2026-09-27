package com.uniquindio.ecommerce.domain.repository;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

import java.util.Optional;
import java.util.UUID;

public interface PrendaDeAutorRepository {

    /**
     * Recupera una prenda de autor por su identificador único de dominio.
     */
    Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id);

    /**
     * Recupera todas las prendas de autor creadas por un diseñador específico.
     */
    Optional<PrendaDeAutor> obtenerPrendaPorCreador(UUID creadorId);

    /**
     * Almacena o actualiza una prenda de autor.
     */
    PrendaDeAutor guardarPrenda(PrendaDeAutor prendaDeAutor);
}
