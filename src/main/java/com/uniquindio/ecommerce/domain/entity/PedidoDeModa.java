package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.valueobject.EstadoPedido;

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

    private PedidoDeModa(UUID id, UUID clienteId, List<VariantePrenda> variantes) {
        this.id = Objects.requireNonNull(id, "El ID del pedido es obligatorio.");
        this.clienteId = Objects.requireNonNull(clienteId, "El ID del cliente es obligatorio.");
        this.variantes = validarVariantes(variantes);
        this.estado = EstadoPedido.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public static PedidoDeModa crear(UUID id, UUID clienteId, List<VariantePrenda> variantes) {
        return new PedidoDeModa(id, clienteId, variantes);
    }

    private List<VariantePrenda> validarVariantes(List<VariantePrenda> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw new IllegalArgumentException("La BolsaDeCompra no puede estar vacía.");
        }
        for (VariantePrenda variante : variantes) {
            if (variante.stockDisponible() <= 0) {
                throw new IllegalArgumentException("Una de las variantes seleccionadas no tiene stock reservado.");
            }
        }
        return List.copyOf(variantes);
    }

    public void autorizarPago() {
        if (this.estado != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("El pedido no está en estado pendiente para autorizar el pago.");
        }
        this.estado = EstadoPedido.PAGADO;
    }

    public void despachar() {
        if (this.estado != EstadoPedido.PAGADO) {
            throw new IllegalStateException("El pedido debe tener el pago autorizado para ser despachado.");
        }
        this.estado = EstadoPedido.DESPACHADO;
    }

    public void completar() {
        if (this.estado != EstadoPedido.DESPACHADO) {
            throw new IllegalStateException("El pedido debe estar despachado para ser completado.");
        }
        this.estado = EstadoPedido.COMPLETADO;
    }

    public void cancelar() {
        if (this.estado == EstadoPedido.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un pedido completado.");
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