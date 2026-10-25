package com.uniquindio.ecommerce.domain.exception;

public class ModificacionBolsaNoPermitidaException extends ReglaDominioException {
    // lanza una excepcion cuando la bolsa ya se convirtió en un pedido
    public ModificacionBolsaNoPermitidaException(String message) {
        super(message);
    }
}
