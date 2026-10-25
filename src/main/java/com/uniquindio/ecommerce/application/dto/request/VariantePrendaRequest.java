package com.uniquindio.ecommerce.application.dto.request;

import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VariantePrendaRequest(
        @NotBlank(message = "El SKU es obligatorio.") @Schema(example = "CAM-LINO-M") String sku,
        @NotNull(message = "La talla es obligatoria.") @Schema(example = "M") TipoTalla tipoTalla,
        @NotNull(message = "El stock de la variante es obligatorio.")
        @PositiveOrZero(message = "El stock de la variante no puede ser negativo.")
        @Schema(example = "5") Integer stockDisponible
) {
}
