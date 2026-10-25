package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.*;
import com.uniquindio.ecommerce.domain.entity.*;
import com.uniquindio.ecommerce.domain.repository.*;
import com.uniquindio.ecommerce.domain.exception.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class ConfirmarPedidoUseCase {
    private final BolsaDeEstiloRepository bolsas;
    private final PrendaDeAutorRepository prendas;
    private final CatalogoVariantesRepository catalogo;
    private final PedidoDeModaRepository pedidos;
    private final UsuarioActual actual;
    @Transactional
    public PedidoDeModa ejecutar(UUID bolsaId) {
        var bolsa=bolsas.obtenerBolsaParaActualizar(new BolsaDeEstiloId(bolsaId))
                .orElseThrow(() -> new RecursoNoEncontradoException("La bolsa no existe."));
        if(!actual.id().equals(bolsa.getClienteId())) throw new AccesoDenegadoException();
        if(!bolsa.estaActiva()) throw new ModificacionBolsaNoPermitidaException("La bolsa está cerrada o ya fue confirmada.");
        if(bolsa.getLineas().isEmpty()) throw new ReglaDominioException("No se puede confirmar una bolsa vacía.");
        Map<UUID,PrendaDeAutorId> referencias=new HashMap<>();
        for(var linea:bolsa.getLineas()) referencias.put(linea.varianteId(),catalogo.prendaDeVariante(linea.varianteId())
                .orElseThrow(() -> new ReglaDominioException("Una variante de la bolsa ya no está disponible.")));
        // Orden global fijo de bloqueo: evita ciclos entre compras de varias prendas.
        Map<PrendaDeAutorId,PrendaDeAutor> bloqueadas=new LinkedHashMap<>();
        referencias.values().stream().distinct().sorted(Comparator.comparing(PrendaDeAutorId::value)).forEach(id ->
            bloqueadas.put(id,prendas.obtenerPrendaParaActualizar(id)
                    .orElseThrow(() -> new ReglaDominioException("Una prenda de la bolsa ya no está disponible."))));
        List<LineaDePedido> historicas=new ArrayList<>();
        for(var linea:bolsa.getLineas()) {
            var prenda=bloqueadas.get(referencias.get(linea.varianteId()));
            if(!prenda.isDisponibleParaLaVenta()) throw new ReglaDominioException("Una prenda está suspendida para la venta.");
            var variante=prenda.getVariantes().stream().filter(v -> v.id().equals(linea.varianteId())).findFirst()
                    .orElseThrow(() -> new ReglaDominioException("Una variante de la bolsa ya no está disponible."));
            var restante=variante.reducirStock(linea.cantidad());
            historicas.add(new LineaDePedido(prenda.getId().value(),variante.id(),prenda.getNombre(),
                    variante.sku(),variante.tipoTalla(),linea.cantidad(),linea.precioUnitario()));
            prenda.actualizarStockVariante(new VariantePrendaId(variante.id()),restante.stockDisponible());
        }
        var pedido=PedidoDeModa.desdeBolsa(UUID.randomUUID(),bolsa.getClienteId(),bolsaId,historicas,LocalDateTime.now(ZoneOffset.UTC));
        bloqueadas.values().forEach(prendas::guardarPrenda);
        bolsa.cerrarBolsa();
        bolsas.guardarBolsa(bolsa);
        return pedidos.registrar(pedido);
    }
}
