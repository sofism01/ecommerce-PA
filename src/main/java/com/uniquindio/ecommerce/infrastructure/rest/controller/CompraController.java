package com.uniquindio.ecommerce.infrastructure.rest.controller;
import com.uniquindio.ecommerce.application.dto.request.AgregarItemBolsaRequest;
import com.uniquindio.ecommerce.application.dto.response.*;
import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController @RequestMapping(value="/api",produces=MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor @Tag(name="Compra",description="Bolsa a pedido PENDIENTE, sin movimientos de dinero")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Datos inválidos",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="404",description="Recurso inexistente",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="409",description="Stock insuficiente, bolsa cerrada/vacía, oferta suspendida o conflicto concurrente",content=@Content(schema=@Schema(implementation=ErrorResponse.class)))})
public class CompraController {
    private final AgregarVarianteABolsaUseCase agregar;
    private final ConfirmarPedidoUseCase confirmar;
    private final ConsultarPedidoUseCase consultar;
    private final BolsaDeEstiloMapper bolsas;
    private final PedidoMapper pedidos;
    @PostMapping(value="/bolsas/{bolsaId}/items",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Agregar una variante a la bolsa propia",description="Sólo varianteId y cantidad. Precio, moneda y stock provienen del catálogo. Acumula cantidades sin reservar stock; se vuelve a validar al confirmar.")
    @ApiResponse(responseCode="200",description="Bolsa actualizada con precios y subtotales",content=@Content(schema=@Schema(implementation=BolsaDeEstiloResponse.class)))
    public BolsaDeEstiloResponse agregar(@PathVariable UUID bolsaId,@Valid @RequestBody AgregarItemBolsaRequest r) {
        return bolsas.toResponse(agregar.ejecutar(bolsaId,r.varianteId(),r.cantidad()));
    }
    @PostMapping("/bolsas/{bolsaId}/confirmar-pedido")
    @Operation(summary="Confirmar la bolsa propia como pedido",description="Crea un pedido PENDIENTE con precios históricos de la bolsa, fecha UTC y total de dominio. Revalida oferta y stock, descuenta variantes y cierra la bolsa en una sola transacción. No realiza pagos. Cerrar una bolsa manualmente no crea un pedido y hace que ya no pueda confirmarse.")
    @ApiResponse(responseCode="201",description="Pedido persistido; Location permite consultarlo",content=@Content(schema=@Schema(implementation=PedidoResponse.class)))
    public ResponseEntity<PedidoResponse> confirmar(@PathVariable UUID bolsaId) {
        var p=confirmar.ejecutar(bolsaId);
        return ResponseEntity.created(URI.create("/api/pedidos/"+p.getId())).body(pedidos.toResponse(p));
    }
    @GetMapping("/pedidos/{pedidoId}")
    @Operation(summary="Consultar un pedido propio",description="Devuelve artículos, cantidades, precios y subtotales históricos aunque el catálogo cambie o se elimine lógicamente.")
    @ApiResponse(responseCode="200",description="Pedido del comprador autenticado",content=@Content(schema=@Schema(implementation=PedidoResponse.class)))
    public PedidoResponse consultar(@PathVariable UUID pedidoId) {
        return pedidos.toResponse(consultar.ejecutar(pedidoId));
    }
}
