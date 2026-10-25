package com.uniquindio.ecommerce.domain.repository;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PrendaDeAutorRepository {
    /** Lectura para una modificación atómica; el adaptador decide el control de concurrencia. */
    default Optional<PrendaDeAutor> obtenerPrendaParaActualizar(PrendaDeAutorId id) {
        return obtenerPrendaPorId(id);
    }

    /**
     * Recupera una prenda de autor por su identificador único de dominio.
     */
    Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id);

    /**
     * Recupera todas las prendas de autor creadas por un diseñador específico.
     */
    List<PrendaDeAutor> obtenerPrendasPorCreador(UUID creadorId);

    /**
     * Almacena o actualiza una prenda de autor.
     */
    PrendaDeAutor guardarPrenda(PrendaDeAutor prendaDeAutor);
}
