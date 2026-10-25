package com.uniquindio.ecommerce.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegistrarPrendaRequest(
        @NotNull(message = "El ID del creador no puede ser nulo.")
        @Schema(example = "11111111-1111-4111-8111-111111111111")
        UUID creadorId,

        @NotBlank(message = "El nombre de la prenda no puede estar vacío.")
        @Schema(example = "Camisa de lino")
        String nombre,

        @NotBlank(message = "La descripción de la prenda no puede estar vacía.")
        @Schema(example = "Camisa confeccionada a mano en lino")
        String descripcion,

        @NotNull(message = "El precio base no puede ser nulo.")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio base debe ser mayor a 0.")
        @Schema(example = "150000.00")
        BigDecimal precioBase,

        @NotBlank(message = "La moneda no puede estar vacía.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO válido de 3 letras.")
        @Schema(example = "COP")
        String moneda,

        @NotNull(message = "La lista de variantes no puede ser nula.")
        @Size(min = 1, message = "Debe haber al menos una variante.")
        List<@NotNull(message = "La variante no puede ser nula.") @Valid VariantePrendaRequest> variantes,

        @NotNull(message = "El stock total es obligatorio.")
        @PositiveOrZero(message = "El stock total no puede ser negativo.")
        @Schema(description = "Stock total del modelo existente; se administra independientemente del stock por variante.", example = "5")
        Integer stockTotal
) {
}
