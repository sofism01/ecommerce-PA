package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.dto.response.*;
import com.uniquindio.ecommerce.application.usecase.ConsultarVariedadCreadoresUseCase;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.ConsultaPrendasMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(value = "/api/reportes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Consultas agregadas del catálogo real")
public class ReporteController {
    private final ConsultarVariedadCreadoresUseCase consultar;
    private final ConsultaPrendasMapper mapper;

    @GetMapping("/creadores/variedad")
    @Operation(summary = "Ranking de creadores por variantes ofertadas", description = "Cuenta variantes no eliminadas de prendas disponibles para venta. Incluye variantes con stock cero, excluye prendas suspendidas o eliminadas. Cuenta prendas distintas con variantes vigentes. Ordena por cantidad de variantes DESC y UUID del creador ASC para empates. Creadores sin variantes ofertadas no aparecen. No representa ventas ni unidades de stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking calculado en H2; [] si no hay oferta", content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = VariedadCreadorResponse.class)),
                    examples = @ExampleObject(value = "[{\"creadorId\":\"81000000-0000-4000-8000-000000000001\",\"cantidadVariantes\":4,\"cantidadPrendas\":2},{\"creadorId\":\"81000000-0000-4000-8000-000000000002\",\"cantidadVariantes\":1,\"cantidadPrendas\":1}]"))),
            @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<VariedadCreadorResponse> variedad() {
        return consultar.ejecutar().stream().map(mapper::toResponse).toList();
    }
}
