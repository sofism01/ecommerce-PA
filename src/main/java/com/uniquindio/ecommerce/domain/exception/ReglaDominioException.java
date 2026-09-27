package com.uniquindio.ecommerce.domain.exception;

public class ReglaDominioException extends RuntimeException {
    // Excepción genérica para violaciones de reglas de dominio
    public ReglaDominioException(String mensaje) {
        super(mensaje);
    }
}