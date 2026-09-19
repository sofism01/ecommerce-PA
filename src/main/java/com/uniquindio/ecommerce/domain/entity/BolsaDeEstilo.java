package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.StockInsuficienteException;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import com.uniquindio.ecommerce.domain.valueobject.Dinero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BolsaDeEstilo {

    private final BolsaDeEstiloId id;
    private final UUID clienteId;
    private final List<LineaDeBolsa> lineas;
    private boolean cerrada;

    public BolsaDeEstilo(BolsaDeEstiloId id, UUID clienteId) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la bolsa no puede ser nulo.");
        }
        if (clienteId == null) {
            throw new IllegalArgumentException("El identificador del cliente no puede ser nulo.");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.lineas = new ArrayList<>();
        this.cerrada = false;
    }

    public void agregarLinea(LineaDeBolsa nuevaLinea, int stockDisponibleEnTaller) {
        validarEstadoActivo();

        if (nuevaLinea.cantidad() > stockDisponibleEnTaller) {
            throw new StockInsuficienteException("No hay suficiente stock en el taller para esta variante de talla.");
        }

        // Si ya existe la línea con la misma variante, acumulamos o actualizamos
        for (int i = 0; i < lineas.size(); i++) {
            LineaDeBolsa lineaExistente = lineas.get(i);
            if (lineaExistente.varianteId().equals(nuevaLinea.varianteId())) {
                int cantidadTotal = lineaExistente.cantidad() + nuevaLinea.cantidad();
                if (cantidadTotal > stockDisponibleEnTaller) {
                    throw new StockInsuficienteException("La cantidad total excede el inventario físico disponible.");
                }
                lineas.set(i, lineaExistente.actualizarCantidad(cantidadTotal));
                return;
            }
        }

        lineas.add(nuevaLinea);
    }

    public void cerrarBolsa() {
        this.cerrada = true;
    }

    private void validarEstadoActivo() {
        if (cerrada) {
            throw new ModificacionBolsaNoPermitidaException("Una bolsa de estilo cerrada o convertida en pedido no puede ser modificada.");
        }
    }

    // --- equals() y hashCode() por identidad (basados únicamente en el id) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BolsaDeEstilo that = (BolsaDeEstilo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- Getters defensivos (sin setters para proteger las invariantes) ---

    public BolsaDeEstiloId getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public List<LineaDeBolsa> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    public boolean isCerrada() {
        return cerrada;
    }
}