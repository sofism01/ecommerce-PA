package com.uniquindio.ecommerce.application.dto.response;

import java.util.List;
import java.util.UUID;

public record PrendaResponse(
        UUID prendaId, UUID creadorId, String nombre, String descripcion,
        DineroResponse precioBase, List<VariantePrendaResponse> variantes,
        int stockTotal, boolean disponibleParaLaVenta
) {
}
