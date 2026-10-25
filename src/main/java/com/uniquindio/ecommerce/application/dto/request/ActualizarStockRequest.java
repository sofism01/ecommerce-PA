package com.uniquindio.ecommerce.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarStockRequest(
        @NotNull(message = "El nuevo stock es obligatorio.")
        @PositiveOrZero(message = "El nuevo stock no puede ser negativo.")
        @Schema(description = "Nueva existencia absoluta de la variante, no una cantidad a sumar.", example = "8")
        Integer nuevoStock
) {
}
