package com.uniquindio.ecommerce.application.dto.response;

import java.util.UUID;

public record LineaDeBolsaResponse(
        UUID varianteId, int cantidad, DineroResponse precioUnitario, DineroResponse subtotal
) {
}
