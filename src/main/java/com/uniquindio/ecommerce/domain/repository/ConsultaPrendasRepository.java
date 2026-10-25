package com.uniquindio.ecommerce.domain.repository;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.query.*;
import java.util.List;

/** Puerto de consultas: separado del contrato de escritura del agregado. */
public interface ConsultaPrendasRepository {
    Pagina<PrendaDeAutor> buscar(ConsultaPrendas consulta);
    List<VariedadCreador> rankingVariedad();
}
