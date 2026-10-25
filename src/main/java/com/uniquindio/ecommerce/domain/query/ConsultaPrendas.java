package com.uniquindio.ecommerce.domain.query;

import java.util.UUID;

/** Criterios de lectura independientes de Spring y JPA. */
public record ConsultaPrendas(String q, UUID creadorId, String moneda, int page, int size) {
    public ConsultaPrendas {
        if (page < 0) throw new ConsultaInvalidaException("page debe ser mayor o igual a 0.");
        if (size < 1 || size > 100) throw new ConsultaInvalidaException("size debe estar entre 1 y 100.");
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new ConsultaInvalidaException("El desplazamiento solicitado excede el máximo admitido.");
        }
        q = q == null || q.isBlank() ? null : q.trim();
        moneda = moneda == null || moneda.isBlank() ? null : moneda.trim();
        if (moneda != null && !moneda.matches("[a-zA-Z]{3}")) {
            throw new ConsultaInvalidaException("moneda debe contener tres letras.");
        }
    }
}
