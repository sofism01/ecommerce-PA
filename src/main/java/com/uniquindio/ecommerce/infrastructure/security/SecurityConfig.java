package com.uniquindio.ecommerce.infrastructure.security;

import com.uniquindio.ecommerce.application.security.*;
import com.uniquindio.ecommerce.domain.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder decoder, UsuarioRepository usuarios,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        var entry = (org.springframework.security.web.AuthenticationEntryPoint) (req,res,ex) -> {
            res.setHeader("WWW-Authenticate","Bearer");
            resolver.resolveException(req,res,null,new CredencialesInvalidasException());
        };
        var denied = (org.springframework.security.web.access.AccessDeniedHandler) (req,res,ex) ->
                resolver.resolveException(req,res,null,new AccesoDenegadoException());
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET,"/api/prendas","/api/prendas/creador/*","/api/reportes/creadores/variedad",
                            "/api-docs","/api-docs/**","/swagger-ui.html","/swagger-ui/**").permitAll()
                    .requestMatchers("/h2-console/**").denyAll()
                    .requestMatchers(HttpMethod.POST,"/api/prendas").hasRole("VENDEDOR")
                    .requestMatchers(HttpMethod.PATCH,"/api/prendas/*/suspender","/api/prendas/*/variantes/*/stock").hasRole("VENDEDOR")
                    .requestMatchers("/api/bolsas","/api/bolsas/**").hasRole("COMPRADOR")
                    .requestMatchers(HttpMethod.GET,"/api/pedidos/*").hasRole("COMPRADOR")
                    .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
                .oauth2ResourceServer(o -> o.authenticationEntryPoint(entry).accessDeniedHandler(denied)
                    .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(jwt -> {
                        var usuario=usuarios.porId(UUID.fromString(jwt.getSubject())).orElse(null);
                        if (usuario == null || !usuario.isActivo() || !usuario.getRol().name().equals(jwt.getClaimAsString("rol"))
                                || !usuario.getCorreo().equals(jwt.getClaimAsString("correo")))
                            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
                        return new JwtAuthenticationToken(jwt,
                                List.of(new SimpleGrantedAuthority("ROLE_"+usuario.getRol().name())),jwt.getSubject());
                    })))
                .build();
    }

    // Excepción aislada al perfil local: no altera las cabeceras ni el CSRF del resto de rutas.
    @Bean @Profile("dev") @Order(0)
    SecurityFilterChain localH2(HttpSecurity http) throws Exception {
        return http.securityMatcher("/h2-console/**").csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .headers(h -> h.frameOptions(f -> f.sameOrigin())).build();
    }
}
