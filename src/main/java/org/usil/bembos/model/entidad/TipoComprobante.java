package org.usil.bembos.model.entidad;

public enum TipoComprobante {
    BOLETA("Boleta de venta", "B001"),
    FACTURA("Factura", "F001");

    private final String etiqueta;
    private final String serie;

    TipoComprobante(String etiqueta, String serie) {
        this.etiqueta = etiqueta;
        this.serie = serie;
    }

    public String getSerie() {
        return serie;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
