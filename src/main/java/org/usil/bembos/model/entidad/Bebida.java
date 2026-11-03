package org.usil.bembos.model.entidad;

import java.math.BigDecimal;

public class Bebida extends Producto {

    public Bebida(int id, String codigo, String nombre, String descripcion, BigDecimal precioBase) {
        super(id, codigo, nombre, descripcion, precioBase);
    }

    @Override
    public TipoProducto getTipo() {
        return TipoProducto.BEBIDA;
    }
}
