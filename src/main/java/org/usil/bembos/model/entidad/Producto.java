package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Producto del catalogo. Las subclases concretas son creadas por
 * {@link org.usil.bembos.model.factory.ProductoFactory} (patron Factory).
 */
public abstract class Producto {

    private final int id;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final BigDecimal precioBase;

    protected Producto(int id, String codigo, String nombre, String descripcion, BigDecimal precioBase) {
        this.id = id;
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion == null ? "" : descripcion;
        if (precioBase == null || precioBase.signum() < 0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo");
        }
        this.precioBase = precioBase;
    }

    public abstract TipoProducto getTipo();

    /** Indica si admite quitar ingredientes o agregar adicionales. */
    public boolean esPersonalizable() {
        return false;
    }

    /** Tamanos en los que puede venderse el producto. */
    public List<Tamano> tamanosPermitidos() {
        return List.of(Tamano.REGULAR, Tamano.AGRANDADO);
    }

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto otro)) return false;
        return codigo.equals(otro.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
