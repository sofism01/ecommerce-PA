package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.dto.request.CrearBolsaRequest;
import com.uniquindio.ecommerce.application.dto.response.BolsaDeEstiloResponse;
import com.uniquindio.ecommerce.application.dto.response.ErrorResponse;
import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.domain.exception.BolsaDeEstiloNoEncontradaException;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.BolsaDeEstiloMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/bolsas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Bolsas de estilo", description = "Creación, consulta, vaciado y cierre de bolsas de los compradores")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class BolsaDeEstiloController {
    private final CrearBolsaDeEstiloUseCase crear;
    private final ObtenerBolsaDeEstiloActivaUseCase obtenerActiva;
    private final VaciarBolsaDeEstiloUseCase vaciar;
    private final CerrarBolsaDeEstiloUseCase cerrar;
    private final BolsaDeEstiloMapper mapper;
    private final AutorizarRecursosUseCase autorizar;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear una bolsa de estilo", description = "Crea una bolsa activa vacía. El caso de uso actual permite varias bolsas activas por cliente.")
    @ApiResponse(responseCode = "201", description = "Bolsa creada", content = @Content(schema = @Schema(implementation = BolsaDeEstiloResponse.class)))
    public ResponseEntity<BolsaDeEstiloResponse> crear(@Valid @RequestBody CrearBolsaRequest request) {
        autorizar.identidad(request.clienteId());
        var bolsa = crear.ejecutar(request.clienteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(bolsa));
    }

    @GetMapping("/activas/{clienteId}")
    @Operation(summary = "Obtener una bolsa activa del cliente", description = "Devuelve una bolsa activa. Si existen varias, el repositorio actual no garantiza cuál selecciona.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bolsa activa", content = @Content(schema = @Schema(implementation = BolsaDeEstiloResponse.class))),
            @ApiResponse(responseCode = "404", description = "El cliente no tiene una bolsa activa", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BolsaDeEstiloResponse obtenerActiva(@Parameter(description = "UUID del cliente", required = true) @PathVariable UUID clienteId) {
        autorizar.identidad(clienteId);
        var bolsa = obtenerActiva.ejecutar(clienteId)
                .orElseThrow(() -> new BolsaDeEstiloNoEncontradaException("El cliente no tiene una bolsa de estilo activa."));
        return mapper.toResponse(bolsa);
    }

    @DeleteMapping("/{bolsaId}/items")
    @Operation(summary = "Vaciar una bolsa activa", description = "Elimina todas las líneas, conservando la bolsa. Una bolsa cerrada no puede vaciarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsa vaciada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bolsa inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Bolsa cerrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> vaciar(@Parameter(description = "UUID de la bolsa", required = true) @PathVariable UUID bolsaId) {
        autorizar.bolsa(bolsaId);
        vaciar.ejecutar(mapper.bolsaId(bolsaId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{bolsaId}/cerrar")
    @Operation(summary = "Cerrar una bolsa de estilo", description = "Bloquea las modificaciones de contenido. No crea un pedido ni procesa pagos. Repetir el cierre conserva el estado cerrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsa cerrada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bolsa inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cerrar(@Parameter(description = "UUID de la bolsa", required = true) @PathVariable UUID bolsaId) {
        autorizar.bolsa(bolsaId);
        cerrar.ejecutar(mapper.bolsaId(bolsaId));
        return ResponseEntity.noContent().build();
    }
}
