package org.usil.bembos.model.entidad;

import java.math.BigDecimal;

/** Acompanamientos: papas fritas, nuggets, aros de cebolla, etc. */
public class Complemento extends Producto {

    public Complemento(int id, String codigo, String nombre, String descripcion, BigDecimal precioBase) {
        super(id, codigo, nombre, descripcion, precioBase);
    }

    @Override
    public TipoProducto getTipo() {
        return TipoProducto.COMPLEMENTO;
    }
}
