package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hamburguesa con receta base. Es el unico producto personalizable:
 * se pueden quitar ingredientes no obligatorios y agregar adicionales.
 */
public class Hamburguesa extends Producto {

    /** Ingrediente de la receta -> true si es obligatorio (no se puede quitar). */
    private final Map<Ingrediente, Boolean> receta = new LinkedHashMap<>();

    public Hamburguesa(int id, String codigo, String nombre, String descripcion, BigDecimal precioBase) {
        super(id, codigo, nombre, descripcion, precioBase);
    }

    @Override
    public TipoProducto getTipo() {
        return TipoProducto.HAMBURGUESA;
    }

    @Override
    public boolean esPersonalizable() {
        return true;
    }

    public void agregarIngredienteReceta(Ingrediente ingrediente, boolean obligatorio) {
        receta.put(ingrediente, obligatorio);
    }

    public Map<Ingrediente, Boolean> getReceta() {
        return Collections.unmodifiableMap(receta);
    }

    public boolean contieneIngrediente(Ingrediente ingrediente) {
        return receta.containsKey(ingrediente);
    }

    public boolean esObligatorio(Ingrediente ingrediente) {
        return Boolean.TRUE.equals(receta.get(ingrediente));
    }
}
