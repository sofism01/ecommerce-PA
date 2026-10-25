package com.uniquindio.ecommerce.domain.query;

import java.util.List;

/** Resultado de lectura; no expone tipos de infraestructura. */
public record Pagina<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
    public Pagina {
        content = List.copyOf(content);
    }
}
