package com.uniquindio.ecommerce.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CrearBolsaRequest(
        @NotNull(message = "El ID del cliente es obligatorio.")
        @Schema(example = "22222222-2222-4222-8222-222222222222") UUID clienteId
) {
}
