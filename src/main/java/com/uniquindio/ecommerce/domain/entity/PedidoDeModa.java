package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;

import com.uniquindio.ecommerce.domain.valueobject.EstadoPedido;
import com.uniquindio.ecommerce.domain.valueobject.LineaDePedido;
import com.uniquindio.ecommerce.domain.valueobject.Dinero;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PedidoDeModa {

    private final UUID id;
    private final UUID clienteId;
    private final List<VariantePrenda> variantes;
    private EstadoPedido estado;
    private final LocalDateTime fechaCreacion;
    private final UUID bolsaId;
    private final List<LineaDePedido> lineas;

    private PedidoDeModa(UUID id, UUID clienteId, List<VariantePrenda> variantes) {
        if (id == null) {
            throw new ReglaDominioException("El ID del pedido es obligatorio.");
        }
        this.id = id;
        if (clienteId == null) {
            throw new ReglaDominioException("El ID del cliente es obligatorio.");
        }
        this.clienteId = clienteId;
        this.variantes = validarVariantes(variantes);
        this.estado = EstadoPedido.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
        this.bolsaId = null;
        this.lineas = List.of();
    }

    /** Modelo histórico completo usado por el flujo de compra y su reconstrucción. */
    private PedidoDeModa(UUID id, UUID clienteId, UUID bolsaId, List<LineaDePedido> lineas,
                         EstadoPedido estado, LocalDateTime fecha) {
        if (id == null || clienteId == null || bolsaId == null || estado == null || fecha == null)
            throw new ReglaDominioException("La identidad, bolsa, estado y fecha del pedido son obligatorios.");
        if (lineas == null || lineas.isEmpty() || lineas.stream().anyMatch(Objects::isNull))
            throw new ReglaDominioException("El pedido debe contener líneas válidas.");
        if (lineas.stream().map(LineaDePedido::varianteId).distinct().count() != lineas.size())
            throw new ReglaDominioException("El pedido no puede repetir variantes.");
        String moneda = lineas.getFirst().precioUnitario().moneda();
        if (lineas.stream().anyMatch(l -> !moneda.equals(l.precioUnitario().moneda())))
            throw new ReglaDominioException("Todas las líneas del pedido deben utilizar la misma moneda.");
        this.id=id; this.clienteId=clienteId; this.bolsaId=bolsaId;
        this.lineas=List.copyOf(lineas); this.estado=estado; this.fechaCreacion=fecha;
        // Se conserva la API original de Entrega 1; las compras usan las líneas históricas.
        this.variantes=List.of();
    }

    public static PedidoDeModa desdeBolsa(UUID id, UUID clienteId, UUID bolsaId,
                                         List<LineaDePedido> lineas, LocalDateTime fecha) {
        return new PedidoDeModa(id, clienteId, bolsaId, lineas, EstadoPedido.PENDIENTE, fecha);
    }

    public static PedidoDeModa reconstruir(UUID id, UUID clienteId, UUID bolsaId,
            List<LineaDePedido> lineas, EstadoPedido estado, LocalDateTime fecha) {
        return new PedidoDeModa(id, clienteId, bolsaId, lineas, estado, fecha);
    }

    public Dinero calcularTotal() {
        if (lineas.isEmpty()) throw new ReglaDominioException("Este pedido no tiene líneas históricas de compra.");
        var monto=lineas.stream().map(l -> l.calcularSubtotal().monto()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dinero(monto, lineas.getFirst().precioUnitario().moneda());
    }

    public UUID getBolsaId() { return bolsaId; }
    public List<LineaDePedido> getLineas() { return lineas; }

    public static PedidoDeModa crear(UUID id, UUID clienteId, List<VariantePrenda> variantes) {
        return new PedidoDeModa(id, clienteId, variantes);
    }

    private List<VariantePrenda> validarVariantes(List<VariantePrenda> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw new ReglaDominioException("La BolsaDeCompra no puede estar vacía.");
        }
        for (VariantePrenda variante : variantes) {
            if (variante == null) {
                throw new ReglaDominioException("Las variantes del pedido no pueden ser nulas.");
            }
            if (variante.stockDisponible() <= 0) {
                throw new ReglaDominioException("Una de las variantes seleccionadas no tiene stock reservado.");
            }
        }
        return List.copyOf(variantes);
    }

    public void autorizarPago() {
        if (this.estado != EstadoPedido.PENDIENTE) {
            throw new ReglaDominioException("El pedido no está en estado pendiente para autorizar el pago.");
        }
        this.estado = EstadoPedido.PAGADO;
    }

    public void despachar() {
        if (this.estado != EstadoPedido.PAGADO) {
            throw new ReglaDominioException("El pedido debe tener el pago autorizado para ser despachado.");
        }
        this.estado = EstadoPedido.DESPACHADO;
    }

    public void completar() {
        if (this.estado != EstadoPedido.DESPACHADO) {
            throw new ReglaDominioException("El pedido debe estar despachado para ser completado.");
        }
        this.estado = EstadoPedido.COMPLETADO;
    }

    public void cancelar() {
        if (this.estado == EstadoPedido.COMPLETADO) {
            throw new ReglaDominioException("No se puede cancelar un pedido completado.");
        }
        this.estado = EstadoPedido.CANCELADO;
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public List<VariantePrenda> getVariantes() {
        return variantes;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    // --- equals y hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PedidoDeModa that = (PedidoDeModa) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
