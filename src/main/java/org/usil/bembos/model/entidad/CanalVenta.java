package org.usil.bembos.model.entidad;

public enum CanalVenta {
    MOSTRADOR("Mostrador"),
    KIOSCO("Kiosco de autoatención"),
    DELIVERY("Delivery");

    private final String etiqueta;

    CanalVenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
