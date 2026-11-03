package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.util.List;

public class Postre extends Producto {

    public Postre(int id, String codigo, String nombre, String descripcion, BigDecimal precioBase) {
        super(id, codigo, nombre, descripcion, precioBase);
    }

    @Override
    public TipoProducto getTipo() {
        return TipoProducto.POSTRE;
    }

    /** Los postres se venden en un solo tamano. */
    @Override
    public List<Tamano> tamanosPermitidos() {
        return List.of(Tamano.REGULAR);
    }
}
