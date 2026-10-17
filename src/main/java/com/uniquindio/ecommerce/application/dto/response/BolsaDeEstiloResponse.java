package com.uniquindio.ecommerce.application.dto.response;

import com.uniquindio.ecommerce.domain.valueobject.LineaDeBolsa;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BolsaDeEstiloResponse(
        @NotNull(message = "El ID de la bolsa no puede ser nulo.")
        UUID bolsaId,

        @NotNull(message = "El ID del cliente no puede ser nulo.")
        UUID clienteId,

        @NotNull(message = "La lista de líneas no puede ser nula.")
        List<LineaDeBolsa> lineas,

        boolean cerrada
) {
}