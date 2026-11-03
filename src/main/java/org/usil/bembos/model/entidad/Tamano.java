package org.usil.bembos.model.entidad;

import java.math.BigDecimal;

/**
 * Tamano de la porcion. El recargo se suma al precio base del producto.
 */
public enum Tamano {
    REGULAR("Regular", new BigDecimal("0.00")),
    AGRANDADO("Agrandado", new BigDecimal("4.50"));

    private final String etiqueta;
    private final BigDecimal recargo;

    Tamano(String etiqueta, BigDecimal recargo) {
        this.etiqueta = etiqueta;
        this.recargo = recargo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public BigDecimal getRecargo() {
        return recargo;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
