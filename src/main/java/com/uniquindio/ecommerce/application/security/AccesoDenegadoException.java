package com.uniquindio.ecommerce.application.security;
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException() { super("No tienes permiso para acceder a este recurso."); }
}
