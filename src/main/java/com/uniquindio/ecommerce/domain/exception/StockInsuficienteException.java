package com.uniquindio.ecommerce.domain.exception;

public class StockInsuficienteException extends ReglaDominioException {
    // lanza una excepcion cuando no hay suficiente stock para un producto que se quiere agregar a la bolsa.
    public StockInsuficienteException(String message) {
        super(message);
    }
}
