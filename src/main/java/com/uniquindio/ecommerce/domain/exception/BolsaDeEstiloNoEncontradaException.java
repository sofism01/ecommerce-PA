package com.uniquindio.ecommerce.domain.exception;

public class BolsaDeEstiloNoEncontradaException extends RuntimeException {
    // lanza una excepcion cuando no se encuentra la bolsa de estilo por su id en la base de datos
    public BolsaDeEstiloNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
