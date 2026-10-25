package com.uniquindio.ecommerce.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.*;
import java.util.List;

@Configuration
@OpenAPIDefinition(info = @Info(title = "BOHEME API", version = "0.0.1",
        description = "Marketplace de ropa de autor: gestión de prendas de los creadores y bolsas de estilo de los compradores. "
                + "Los datos se almacenan en H2 en memoria y se pierden al reiniciar. "
                + "JWT stateless con roles COMPRADOR y VENDEDOR. El UUID de la cuenta corresponde al creadorId o clienteId."))
@SecurityScheme(name="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT",
        description="Pega sólo el JWT devuelto por /api/auth/login, sin el prefijo Bearer.")
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer seguridadOperaciones() {
        return api -> api.getPaths().forEach((path,item) -> item.readOperationsMap().forEach((method,op) -> {
            boolean protegida=path.startsWith("/api/bolsas") || path.startsWith("/api/pedidos")
                    || (path.startsWith("/api/prendas") && !method.name().equals("GET"));
            op.setSecurity(protegida ? List.of(new SecurityRequirement().addList("bearerAuth")) : List.of());
            if (protegida) {
                op.getResponses().addApiResponse("401",error("Token ausente, inválido o vencido"));
                op.getResponses().addApiResponse("403",error("Rol insuficiente o recurso ajeno"));
                String rol=path.startsWith("/api/bolsas") || path.startsWith("/api/pedidos") ? "COMPRADOR" : "VENDEDOR";
                op.setDescription((op.getDescription()==null ? "" : op.getDescription())
                        + " Requiere rol "+rol+" y propiedad del recurso. UUID ajeno devuelve 403.");
            }
        }));
    }
    private static ApiResponse error(String descripcion) {
        return new ApiResponse().description(descripcion).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
    }
}
