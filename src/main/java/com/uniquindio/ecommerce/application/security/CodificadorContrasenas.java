package com.uniquindio.ecommerce.application.security;
public interface CodificadorContrasenas {
    String codificar(String password);
    boolean verificar(String password, String hash);
}
