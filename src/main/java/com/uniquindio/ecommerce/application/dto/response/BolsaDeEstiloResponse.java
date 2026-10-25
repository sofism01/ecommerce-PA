package com.uniquindio.ecommerce.application.dto.response;

import java.util.List;
import java.util.UUID;

public record BolsaDeEstiloResponse(
        UUID bolsaId,

        UUID clienteId,

        List<LineaDeBolsaResponse> lineas,

        boolean cerrada
) {
}
