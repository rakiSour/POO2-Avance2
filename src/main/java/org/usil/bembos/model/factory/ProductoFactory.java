package org.usil.bembos.model.factory;

import org.usil.bembos.model.entidad.Bebida;
import org.usil.bembos.model.entidad.Complemento;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Postre;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.TipoProducto;

import java.math.BigDecimal;

/**
 * <b>Patron Factory (creacional).</b>
 * Centraliza la creacion de productos: la capa DAO solo conoce el tipo
 * almacenado en la base de datos (columna {@code tipo}) y la fabrica decide
 * que subclase concreta instanciar.
 */
public final class ProductoFactory {

    private ProductoFactory() {
    }

    public static Producto crear(TipoProducto tipo, int id, String codigo, String nombre,
                                 String descripcion, BigDecimal precio) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de producto es obligatorio");
        }
        return switch (tipo) {
            case HAMBURGUESA -> new Hamburguesa(id, codigo, nombre, descripcion, precio);
            case COMPLEMENTO -> new Complemento(id, codigo, nombre, descripcion, precio);
            case BEBIDA -> new Bebida(id, codigo, nombre, descripcion, precio);
            case POSTRE -> new Postre(id, codigo, nombre, descripcion, precio);
        };
    }

    /** Variante usada al leer la columna de texto de la base de datos. */
    public static Producto crear(String tipo, int id, String codigo, String nombre,
                                 String descripcion, BigDecimal precio) {
        try {
            return crear(TipoProducto.valueOf(tipo.trim().toUpperCase()), id, codigo, nombre, descripcion, precio);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Tipo de producto desconocido: " + tipo, e);
        }
    }
}
