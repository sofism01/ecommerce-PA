package com.uniquindio.ecommerce.application.security;
import com.uniquindio.ecommerce.domain.entity.Usuario;
import com.uniquindio.ecommerce.application.dto.response.LoginResponse;
public interface EmisorTokens { LoginResponse emitir(Usuario usuario); }
