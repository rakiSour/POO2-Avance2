package org.usil.bembos.model.entidad;

/**
 * Familias de productos que ofrece la franquicia.
 */
public enum TipoProducto {
    HAMBURGUESA("Hamburguesas"),
    COMPLEMENTO("Complementos"),
    BEBIDA("Bebidas"),
    POSTRE("Postres");

    private final String etiqueta;

    TipoProducto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
