package com.uniquindio.ecommerce.domain.entity;

import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;

import com.uniquindio.ecommerce.domain.exception.ModificacionBolsaNoPermitidaException;
import com.uniquindio.ecommerce.domain.exception.StockInsuficienteException;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import com.uniquindio.ecommerce.domain.valueobject.LineaDeBolsa;

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

    private BolsaDeEstilo(BolsaDeEstiloId id, UUID clienteId) {
        if (id == null) {
            throw new ReglaDominioException("El identificador de la bolsa no puede ser nulo.");
        }
        if (clienteId == null) {
            throw new ReglaDominioException("El identificador del cliente no puede ser nulo.");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.lineas = new ArrayList<>();
        this.cerrada = false;
    }

    public static BolsaDeEstilo crear(BolsaDeEstiloId id, UUID clienteId) {
        return new BolsaDeEstilo(id, clienteId);
    }

    /**
     * Reconstruye la selección histórica. El stock se comprueba al agregar, no al leer:
     * el inventario puede haber cambiado desde que se creó la línea.
     */
    public static BolsaDeEstilo reconstruir(BolsaDeEstiloId id, UUID clienteId,
            List<LineaDeBolsa> lineas, boolean cerrada) {
        BolsaDeEstilo bolsa = new BolsaDeEstilo(id, clienteId);
        if (lineas == null || lineas.stream().anyMatch(Objects::isNull)) {
            throw new ReglaDominioException("Las líneas de la bolsa no pueden ser nulas.");
        }
        if (lineas.stream().distinct().count() != lineas.size()) {
            throw new ReglaDominioException("La bolsa no puede contener líneas duplicadas de una variante.");
        }
        for (LineaDeBolsa linea : lineas) {
            bolsa.validarMoneda(linea);
            bolsa.lineas.add(linea);
        }
        bolsa.cerrada = cerrada;
        return bolsa;
    }

    public void agregarLinea(LineaDeBolsa nuevaLinea, int stockDisponibleEnTaller) {
        validarEstadoActivo();

        if (nuevaLinea == null) {
            throw new ReglaDominioException("La línea de bolsa es obligatoria.");
        }
        if (stockDisponibleEnTaller < 0) {
            throw new ReglaDominioException("El stock disponible no puede ser negativo.");
        }
        validarMoneda(nuevaLinea);
        if (nuevaLinea.cantidad() > stockDisponibleEnTaller) {
            throw new StockInsuficienteException("No hay suficiente stock en el taller para esta variante de talla.");
        }

        for (int i = 0; i < lineas.size(); i++) {
            LineaDeBolsa lineaExistente = lineas.get(i);
            if (lineaExistente.varianteId().equals(nuevaLinea.varianteId())) {
                long cantidadTotal = (long) lineaExistente.cantidad() + nuevaLinea.cantidad();
                if (cantidadTotal > stockDisponibleEnTaller) {
                    throw new StockInsuficienteException("La cantidad total excede el inventario físico disponible.");
                }
                lineas.set(i, lineaExistente.actualizarCantidad((int) cantidadTotal));
                return;
            }
        }

        lineas.add(nuevaLinea);
    }

    public void cerrarBolsa() {
        this.cerrada = true;
    }

    private void validarMoneda(LineaDeBolsa nuevaLinea) {
        if (lineas.stream().anyMatch(linea -> !linea.precioUnitario().moneda().equals(nuevaLinea.precioUnitario().moneda()))) {
            throw new ReglaDominioException("Todas las líneas de la bolsa deben utilizar la misma moneda.");
        }
    }

    private void validarEstadoActivo() {
        if (cerrada) {
            throw new ModificacionBolsaNoPermitidaException("Una bolsa de estilo cerrada o convertida en pedido no puede ser modificada.");
        }
    }

    public boolean estaActiva() {
        return !cerrada;
    }

    public void vaciar() {
        validarEstadoActivo();
        this.lineas.clear();
    }

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
