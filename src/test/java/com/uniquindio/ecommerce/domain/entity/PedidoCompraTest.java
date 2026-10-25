package com.uniquindio.ecommerce.domain.entity;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PedidoCompraTest {
    private LineaDePedido linea(int cantidad,String precio,String moneda) {
        return new LineaDePedido(UUID.randomUUID(),UUID.randomUUID(),"Camisa",new SKU("CAM-M"),TipoTalla.M,
                cantidad,new Dinero(new BigDecimal(precio),moneda));
    }
    @Test void totalYSubtotalesSeCalculanEnDominioSinMarcarPagado() {
        // Arrange
        var primera=linea(2,"19.9500","COP"); var segunda=linea(3,"10.00","COP");
        // Act
        var pedido=PedidoDeModa.desdeBolsa(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                List.of(primera,segunda),LocalDateTime.now());
        // Assert
        assertEquals(EstadoPedido.PENDIENTE,pedido.getEstado());
        assertEquals(new BigDecimal("39.9000"),primera.calcularSubtotal().monto());
        assertEquals(new Dinero(new BigDecimal("69.9000"),"COP"),pedido.calcularTotal());
        assertThrows(UnsupportedOperationException.class,() -> pedido.getLineas().clear());
    }
    @Test void reconstruccionNoOmiteInvariantesDeLineasMonedaEIdentidad() {
        // Arrange
        var linea=linea(1,"10","COP"); var fecha=LocalDateTime.now();
        // Act & Assert
        assertThrows(ReglaDominioException.class,() -> PedidoDeModa.desdeBolsa(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),List.of(),fecha));
        assertThrows(ReglaDominioException.class,() -> PedidoDeModa.desdeBolsa(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),List.of(linea,linea),fecha));
        assertThrows(ReglaDominioException.class,() -> PedidoDeModa.reconstruir(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),List.of(linea,linea(1,"5","USD")),EstadoPedido.PENDIENTE,fecha));
        assertThrows(ReglaDominioException.class,() -> PedidoDeModa.desdeBolsa(null,UUID.randomUUID(),UUID.randomUUID(),List.of(linea),fecha));
        assertThrows(ReglaDominioException.class,() -> linea(0,"5","COP"));
        assertThrows(ReglaDominioException.class,() -> linea(1,"0","COP"));
    }
}
