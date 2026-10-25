package com.uniquindio.ecommerce.application.security;
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() { super("Credenciales inválidas."); }
}
