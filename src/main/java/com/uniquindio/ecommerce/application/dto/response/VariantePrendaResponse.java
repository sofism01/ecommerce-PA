package com.uniquindio.ecommerce.application.dto.response;

import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import java.util.UUID;

public record VariantePrendaResponse(UUID varianteId, String sku, TipoTalla tipoTalla, int stockDisponible) {
}
