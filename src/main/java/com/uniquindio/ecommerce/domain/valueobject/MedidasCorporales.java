package com.uniquindio.ecommerce.domain.valueobject;

public record MedidasCorporales(double pechoCm, double cinturaCm, double caderaCm) {
    public MedidasCorporales {
        if (pechoCm <= 0 || cinturaCm <= 0 || caderaCm <= 0) {
            throw new IllegalArgumentException("Las medidas corporales deben ser mayores a cero.");
        }
    }
}