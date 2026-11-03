package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Linea de un pedido. Es inmutable y solo puede crearse mediante
 * {@link org.usil.bembos.model.builder.ItemPedidoBuilder} (patron Builder),
 * que valida todas las reglas de personalizacion antes de instanciarla.
 */
public final class ItemPedido {

    private final Producto producto;
    private final int cantidad;
    private final Tamano tamano;
    private final List<Ingrediente> ingredientesQuitados;
    private final List<Ingrediente> extras;
    private final String nota;
    private final BigDecimal descuento;
    private final String etiquetaCombo;

    /**
     * Constructor de uso interno del Builder. No debe invocarse directamente
     * desde la Vista ni el Controlador.
     */
    public ItemPedido(Producto producto, int cantidad, Tamano tamano,
                      List<Ingrediente> ingredientesQuitados, List<Ingrediente> extras,
                      String nota, BigDecimal descuento, String etiquetaCombo) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.tamano = tamano;
        this.ingredientesQuitados = List.copyOf(ingredientesQuitados);
        this.extras = List.copyOf(extras);
        this.nota = nota == null ? "" : nota;
        this.descuento = descuento == null ? BigDecimal.ZERO : descuento;
        this.etiquetaCombo = etiquetaCombo;
    }

    /** Precio de una unidad: base + recargo de tamano + adicionales. */
    public BigDecimal precioUnitario() {
        BigDecimal totalExtras = extras.stream()
                .map(Ingrediente::precioExtra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return producto.getPrecioBase()
                .add(tamano.getRecargo())
                .add(totalExtras)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotalBruto() {
        return precioUnitario().multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotal() {
        return subtotalBruto().subtract(descuento).setScale(2, RoundingMode.HALF_UP);
    }

    /** Texto que se imprime en la comanda de cocina (KDS). */
    public String descripcionComanda() {
        StringBuilder sb = new StringBuilder();
        sb.append(cantidad).append(" x ").append(producto.getNombre());
        if (tamano != Tamano.REGULAR) {
            sb.append(" (").append(tamano.getEtiqueta()).append(')');
        }
        if (etiquetaCombo != null) {
            sb.append(" [").append(etiquetaCombo).append(']');
        }
        if (!ingredientesQuitados.isEmpty()) {
            sb.append(" | SIN: ").append(nombres(ingredientesQuitados));
        }
        if (!extras.isEmpty()) {
            sb.append(" | EXTRA: ").append(nombres(extras));
        }
        if (!nota.isBlank()) {
            sb.append(" | Nota: ").append(nota);
        }
        return sb.toString();
    }

    private static String nombres(List<Ingrediente> lista) {
        return lista.stream().map(Ingrediente::nombre).collect(Collectors.joining(", "));
    }

    public boolean estaPersonalizado() {
        return !ingredientesQuitados.isEmpty() || !extras.isEmpty();
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Tamano getTamano() {
        return tamano;
    }

    public List<Ingrediente> getIngredientesQuitados() {
        return ingredientesQuitados;
    }

    public List<Ingrediente> getExtras() {
        return extras;
    }

    public String getNota() {
        return nota;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public String getEtiquetaCombo() {
        return etiquetaCombo;
    }

    @Override
    public String toString() {
        return descripcionComanda();
    }
}
