package com.uniquindio.ecommerce.application.dto.response;
import com.uniquindio.ecommerce.domain.valueobject.TipoTalla;
import java.util.UUID;
public record LineaPedidoResponse(UUID prendaId,UUID varianteId,String nombrePrenda,String sku,TipoTalla tipoTalla,
                                  int cantidad,DineroResponse precioUnitario,DineroResponse subtotal) {}
