package com.uniquindio.ecommerce.application.dto.response;
import com.uniquindio.ecommerce.domain.valueobject.EstadoPedido;
import java.time.LocalDateTime;
import java.util.*;
public record PedidoResponse(UUID pedidoId,UUID bolsaId,UUID clienteId,EstadoPedido estado,LocalDateTime fechaCreacion,
                             List<LineaPedidoResponse> lineas,DineroResponse total,String moneda) {}
