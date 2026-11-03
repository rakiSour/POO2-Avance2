package org.usil.bembos.model.entidad;

public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta débito/crédito"),
    BILLETERA_DIGITAL("Billetera digital (Yape/Plin)");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
