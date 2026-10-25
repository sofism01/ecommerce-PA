package com.uniquindio.ecommerce.domain.query;

public class ConsultaInvalidaException extends IllegalArgumentException {
    public ConsultaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
