package com.uniquindio.ecommerce.application.dto.request;

import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegistrarPrendaRequest(
        @NotNull(message = "El ID del creador no puede ser nulo.")
        UUID creadorId,

        @NotBlank(message = "El nombre de la prenda no puede estar vacío.")
        String nombre,

        @NotBlank(message = "La descripción de la prenda no puede estar vacía.")
        String descripcion,

        @NotNull(message = "El precio base no puede ser nulo.")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio base debe ser mayor a 0.")
        BigDecimal precioBase,

        @NotBlank(message = "La moneda no puede estar vacía.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO válido de 3 letras.")
        String moneda,

        @NotNull(message = "La lista de variantes no puede ser nula.")
        @Size(min = 1, message = "Debe haber al menos una variante.")
        List<VariantePrenda> variantes
) {
}