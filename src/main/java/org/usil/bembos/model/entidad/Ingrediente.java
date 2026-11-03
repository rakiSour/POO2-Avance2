package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Insumo que forma parte de la receta de una hamburguesa o que puede
 * agregarse como adicional (extra).
 *
 * @param id          identificador en la base de datos
 * @param nombre      nombre visible del ingrediente
 * @param precioExtra precio cuando se agrega como adicional
 * @param disponible  false cuando no hay stock en tienda
 */
public record Ingrediente(int id, String nombre, BigDecimal precioExtra, boolean disponible) {

    public Ingrediente {
        Objects.requireNonNull(nombre, "El nombre del ingrediente es obligatorio");
        precioExtra = precioExtra == null ? BigDecimal.ZERO : precioExtra;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
