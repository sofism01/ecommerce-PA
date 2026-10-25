package com.uniquindio.ecommerce.domain.exception;

/** Recurso de negocio que no existe; sin dependencias del transporte HTTP. */
public class RecursoNoEncontradoException extends ReglaDominioException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
