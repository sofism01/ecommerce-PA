package com.uniquindio.ecommerce.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "DATOS_INVALIDOS") String code,
        @Schema(example = "La solicitud contiene datos inválidos.") String message,
        @Schema(example = "/api/prendas") String path,
        @Schema(description = "Errores por campo; vacío cuando no corresponde.") Map<String, String> details
) {
}
