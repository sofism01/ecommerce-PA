package com.uniquindio.ecommerce.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AgregarItemBolsaRequest(
        @NotNull(message = "El ID del cliente no puede ser nulo.")
        UUID clienteId,

        @NotNull(message = "El ID de la variante no puede ser nulo.")
        UUID varianteId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        int cantidad
) {
}
