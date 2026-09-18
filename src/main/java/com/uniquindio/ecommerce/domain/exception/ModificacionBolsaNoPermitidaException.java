package com.uniquindio.ecommerce.domain.exception;

public class ModificacionBolsaNoPermitidaException extends RuntimeException {
    // lanza una excepcion cuando la bolsa ya se convirtió en un pedido
    public ModificacionBolsaNoPermitidaException(String message) {
        super(message);
    }
}
