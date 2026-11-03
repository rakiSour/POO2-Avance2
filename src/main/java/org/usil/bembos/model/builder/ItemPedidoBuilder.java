package org.usil.bembos.model.builder;

import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <b>Patron Builder (creacional).</b>
 * Ensambla paso a paso una linea de pedido personalizada: tamano, cantidad,
 * ingredientes retirados, adicionales y notas para cocina. Toda la validacion
 * de la personalizacion ocurre en {@link #build()}, de modo que ni el
 * Controlador ni la Vista tienen que conocer las reglas de la receta.
 *
 * <pre>
 * ItemPedido item = new ItemPedidoBuilder(royal)
 *         .tamano(Tamano.AGRANDADO)
 *         .sin(cebolla)
 *         .conExtra(tocino)
 *         .nota("Bien cocida")
 *         .build();
 * </pre>
 */
public final class ItemPedidoBuilder {

    public static final int MAX_EXTRAS = 6;
    public static final int MAX_CANTIDAD = 20;
    public static final int MAX_LONGITUD_NOTA = 120;

    private final Producto producto;
    private int cantidad = 1;
    private Tamano tamano = Tamano.REGULAR;
    private final Set<Ingrediente> quitados = new LinkedHashSet<>();
    private final List<Ingrediente> extras = new ArrayList<>();
    private String nota = "";
    private BigDecimal descuento = BigDecimal.ZERO;
    private String etiquetaCombo;

    public ItemPedidoBuilder(Producto producto) {
        this.producto = Objects.requireNonNull(producto, "Debe seleccionar un producto");
    }

    public static ItemPedidoBuilder de(Producto producto) {
        return new ItemPedidoBuilder(producto);
    }

    public ItemPedidoBuilder cantidad(int cantidad) {
        this.cantidad = cantidad;
        return this;
    }

    public ItemPedidoBuilder tamano(Tamano tamano) {
        this.tamano = tamano == null ? Tamano.REGULAR : tamano;
        return this;
    }

    /** Retira un ingrediente de la receta base. */
    public ItemPedidoBuilder sin(Ingrediente ingrediente) {
        quitados.add(Objects.requireNonNull(ingrediente));
        return this;
    }

    /** Agrega un adicional (puede repetirse: doble queso = dos llamadas). */
    public ItemPedidoBuilder conExtra(Ingrediente ingrediente) {
        extras.add(Objects.requireNonNull(ingrediente));
        return this;
    }

    public ItemPedidoBuilder conExtra(Ingrediente ingrediente, int veces) {
        for (int i = 0; i < veces; i++) {
            conExtra(ingrediente);
        }
        return this;
    }

    public ItemPedidoBuilder nota(String nota) {
        this.nota = nota == null ? "" : nota.trim();
        return this;
    }

    /** Usado por el Director para aplicar el descuento de combo. */
    public ItemPedidoBuilder descuento(BigDecimal descuento) {
        this.descuento = descuento == null ? BigDecimal.ZERO : descuento;
        return this;
    }

    public ItemPedidoBuilder enCombo(String nombreCombo) {
        this.etiquetaCombo = nombreCombo;
        return this;
    }

    /** Valida todas las reglas y crea el objeto inmutable. */
    public ItemPedido build() {
        List<String> errores = validar();
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException(errores);
        }
        return new ItemPedido(producto, cantidad, tamano, new ArrayList<>(quitados), extras,
                nota, descuento, etiquetaCombo);
    }

    private List<String> validar() {
        List<String> errores = new ArrayList<>();

        if (cantidad < 1 || cantidad > MAX_CANTIDAD) {
            errores.add("La cantidad debe estar entre 1 y " + MAX_CANTIDAD + ".");
        }
        if (!producto.tamanosPermitidos().contains(tamano)) {
            errores.add(producto.getNombre() + " no se vende en tamaño " + tamano.getEtiqueta() + ".");
        }
        if (nota.length() > MAX_LONGITUD_NOTA) {
            errores.add("La nota para cocina no puede superar " + MAX_LONGITUD_NOTA + " caracteres.");
        }
        if (descuento.signum() < 0) {
            errores.add("El descuento no puede ser negativo.");
        }

        boolean personaliza = !quitados.isEmpty() || !extras.isEmpty();
        if (personaliza && !producto.esPersonalizable()) {
            errores.add(producto.getNombre() + " no admite personalización de ingredientes.");
            return errores;
        }

        if (producto instanceof Hamburguesa hamburguesa) {
            for (Ingrediente ing : quitados) {
                if (!hamburguesa.contieneIngrediente(ing)) {
                    errores.add("No se puede quitar " + ing.nombre() + ": no forma parte de la receta.");
                } else if (hamburguesa.esObligatorio(ing)) {
                    errores.add("No se puede quitar " + ing.nombre() + ": es un ingrediente obligatorio.");
                }
            }
        }
        if (extras.size() > MAX_EXTRAS) {
            errores.add("Máximo " + MAX_EXTRAS + " adicionales por producto.");
        }
        for (Ingrediente ing : new LinkedHashSet<>(extras)) {
            if (!ing.disponible()) {
                errores.add("El adicional " + ing.nombre() + " no está disponible.");
            }
            if (quitados.contains(ing)) {
                errores.add("Contradicción: se pidió quitar y agregar " + ing.nombre() + ".");
            }
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        BigDecimal bruto = producto.getPrecioBase().add(tamano.getRecargo())
                .add(extras.stream().map(Ingrediente::precioExtra).reduce(BigDecimal.ZERO, BigDecimal::add))
                .multiply(BigDecimal.valueOf(cantidad));
        if (descuento.compareTo(bruto) > 0) {
            errores.add("El descuento no puede superar el importe del producto.");
        }
        return errores;
    }
}
