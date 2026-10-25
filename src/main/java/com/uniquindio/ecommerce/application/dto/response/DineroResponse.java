package com.uniquindio.ecommerce.application.dto.response;

import java.math.BigDecimal;

public record DineroResponse(BigDecimal monto, String moneda) {
}
