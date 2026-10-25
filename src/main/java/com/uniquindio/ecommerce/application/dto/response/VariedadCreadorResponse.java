package com.uniquindio.ecommerce.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record VariedadCreadorResponse(
        @Schema(example = "81000000-0000-4000-8000-000000000001") UUID creadorId,
        @Schema(description = "Variantes no eliminadas de prendas disponibles para venta; incluye stock cero. No representa ventas.", example = "4") long cantidadVariantes,
        @Schema(description = "Prendas distintas disponibles con al menos una variante no eliminada.", example = "2") long cantidadPrendas) {
}
