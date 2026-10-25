package com.uniquindio.ecommerce.infrastructure.rest.controller;

import com.uniquindio.ecommerce.application.dto.request.ActualizarStockRequest;
import com.uniquindio.ecommerce.application.dto.request.RegistrarPrendaRequest;
import com.uniquindio.ecommerce.application.dto.response.ErrorResponse;
import com.uniquindio.ecommerce.application.dto.response.PrendaResponse;
import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.PrendaMapper;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.ConsultaPrendasMapper;
import com.uniquindio.ecommerce.application.dto.response.PaginaPrendasResponse;
import com.uniquindio.ecommerce.domain.query.ConsultaPrendas;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/prendas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Prendas", description = "Registro, consulta y mantenimiento de prendas de autor")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class PrendaController {
    private final RegistrarPrendaDeAutorUseCase registrar;
    private final ListarPrendasPorCreadorUseCase listar;
    private final SuspenderVentaPrendaDeAutorUseCase suspender;
    private final ActualizarStockVariantePrendaUseCase actualizarStock;
    private final PrendaMapper mapper;
    private final BuscarPrendasUseCase buscar;
    private final ConsultaPrendasMapper consultaMapper;
    private final AutorizarRecursosUseCase autorizar;

    @GetMapping
    @Operation(summary = "Buscar prendas con paginación", description = "Busca por nombre o descripción sin distinguir mayúsculas. Combina filtros con AND. Excluye eliminadas, incluye suspendidas. Orden: nombre ASC, ID ASC. q vacío omite la búsqueda; %, _ y ! se buscan literalmente. Página inicial 0; tamaño de 1 a 100.")
    @ApiResponse(responseCode = "200", description = "Página de prendas, también sin coincidencias o fuera del rango de páginas", content = @Content(
            schema = @Schema(implementation = PaginaPrendasResponse.class),
            examples = @ExampleObject(name = "Sin coincidencias", value = "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"page\":0,\"size\":20}")))
    public PaginaPrendasResponse buscar(
            @Parameter(description = "Texto contenido en nombre o descripción", example = "lino") @RequestParam(required = false) String q,
            @Parameter(description = "UUID del creador", example = "81000000-0000-4000-8000-000000000001") @RequestParam(required = false) UUID creadorId,
            @Parameter(description = "Código de moneda de tres letras, sin distinguir mayúsculas", example = "COP") @RequestParam(required = false) String moneda,
            @Parameter(description = "Página desde 0", schema = @Schema(minimum = "0", defaultValue = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Registros por página (1 a 100)", schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20")) @RequestParam(defaultValue = "20") int size) {
        return consultaMapper.toResponse(buscar.ejecutar(new ConsultaPrendas(q, creadorId, moneda, page, size)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar una prenda de autor", description = "Genera los identificadores de la prenda y sus variantes. Devuelve esos IDs para las operaciones posteriores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prenda registrada", content = @Content(schema = @Schema(implementation = PrendaResponse.class))),
            @ApiResponse(responseCode = "409", description = "Regla de dominio incumplida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PrendaResponse> registrar(@Valid @RequestBody RegistrarPrendaRequest request) {
        autorizar.identidad(request.creadorId());
        var prenda = registrar.ejecutar(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(prenda));
    }

    @GetMapping("/creador/{creadorId}")
    @Operation(summary = "Listar todas las prendas de un creador", description = "Devuelve una colección, incluyendo prendas suspendidas. Sin coincidencias devuelve []. No comprueba la existencia de una cuenta de creador.")
    @ApiResponse(responseCode = "200", description = "Colección de prendas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PrendaResponse.class))))
    public List<PrendaResponse> listar(@Parameter(description = "UUID del creador", required = true) @PathVariable UUID creadorId) {
        return listar.ejecutar(creadorId).stream().map(mapper::toResponse).toList();
    }

    @PatchMapping("/{prendaId}/suspender")
    @Operation(summary = "Suspender la venta de una prenda", description = "Conserva la prenda y marca su venta como suspendida. Suspender una prenda ya suspendida incumple una regla del dominio.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Venta suspendida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Prenda inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Prenda ya suspendida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> suspender(@Parameter(description = "UUID de la prenda", required = true) @PathVariable UUID prendaId) {
        autorizar.prenda(prendaId);
        suspender.ejecutar(mapper.prendaId(prendaId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{prendaId}/variantes/{varianteId}/stock", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar el stock de una variante", description = "Establece el nuevo stock absoluto de la variante. No modifica el stock total independiente del modelo actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock actualizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Prenda o variante inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Regla de dominio incumplida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> actualizarStock(
            @Parameter(description = "UUID de la prenda", required = true) @PathVariable UUID prendaId,
            @Parameter(description = "UUID de una variante de esa prenda", required = true) @PathVariable UUID varianteId,
            @Valid @RequestBody ActualizarStockRequest request) {
        autorizar.prenda(prendaId);
        actualizarStock.ejecutar(mapper.prendaId(prendaId), mapper.varianteId(varianteId), request.nuevoStock());
        return ResponseEntity.noContent().build();
    }
}
