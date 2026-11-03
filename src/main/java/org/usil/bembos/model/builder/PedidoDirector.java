package org.usil.bembos.model.builder;

import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <b>Director del patron Builder.</b>
 * Conoce las "recetas" de los combos de la carta y dirige a los builders
 * ({@link ItemPedidoBuilder} y {@link PedidoBuilder}) para construirlos
 * siempre de la misma manera, aplicando el descuento del combo.
 */
public class PedidoDirector {

    /** Definicion de un combo: codigos de productos que lo componen y descuento. */
    public record DefinicionCombo(String nombre, List<String> codigos, BigDecimal porcentajeDescuento) {
    }

    public static final Map<String, DefinicionCombo> COMBOS = new LinkedHashMap<>();

    static {
        registrar(new DefinicionCombo("Combo Clásico", List.of("HAM-CLA", "COM-PAP", "BEB-GAS"), new BigDecimal("0.15")));
        registrar(new DefinicionCombo("Combo Royal", List.of("HAM-ROY", "COM-PAP", "BEB-GAS"), new BigDecimal("0.15")));
        registrar(new DefinicionCombo("Combo Cheese", List.of("HAM-CHE", "COM-PAP", "BEB-GAS"), new BigDecimal("0.15")));
        registrar(new DefinicionCombo("Combo Crispy", List.of("HAM-CRI", "COM-NUG", "BEB-LIM"), new BigDecimal("0.12")));
        registrar(new DefinicionCombo("Combo Dúo", List.of("HAM-CLA", "HAM-CHE", "COM-PAP", "COM-PAP", "BEB-GAS", "BEB-GAS"),
                new BigDecimal("0.20")));
    }

    private static void registrar(DefinicionCombo combo) {
        COMBOS.put(combo.nombre(), combo);
    }

    private final Function<String, Producto> buscador;

    public PedidoDirector(List<Producto> catalogo) {
        Map<String, Producto> porCodigo = catalogo.stream()
                .collect(Collectors.toMap(Producto::getCodigo, p -> p, (a, b) -> a));
        this.buscador = porCodigo::get;
    }

    public List<String> combosDisponibles() {
        return List.copyOf(COMBOS.keySet());
    }

    /**
     * Construye el combo indicado y agrega sus lineas al pedido en curso.
     *
     * @return las lineas agregadas
     */
    public List<ItemPedido> construirCombo(String nombreCombo, Tamano tamano, PedidoBuilder pedido) {
        DefinicionCombo combo = COMBOS.get(nombreCombo);
        if (combo == null) {
            throw new ReglaNegocioException("El combo '" + nombreCombo + "' no existe.");
        }
        List<ItemPedido> lineas = combo.codigos().stream()
                .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> construirLinea(e.getKey(), e.getValue().intValue(), tamano, combo))
                .toList();
        lineas.forEach(pedido::agregarItem);
        return lineas;
    }

    private ItemPedido construirLinea(String codigo, int cantidad, Tamano tamano, DefinicionCombo combo) {
        Producto producto = buscador.apply(codigo);
        if (producto == null) {
            throw new ReglaNegocioException("El producto " + codigo + " del " + combo.nombre() + " no está en el catálogo.");
        }
        Tamano tamanoLinea = producto.tamanosPermitidos().contains(tamano) ? tamano : Tamano.REGULAR;
        BigDecimal bruto = producto.getPrecioBase().add(tamanoLinea.getRecargo())
                .multiply(BigDecimal.valueOf(cantidad));
        BigDecimal descuento = bruto.multiply(combo.porcentajeDescuento()).setScale(2, RoundingMode.HALF_UP);
        return ItemPedidoBuilder.de(producto)
                .cantidad(cantidad)
                .tamano(tamanoLinea)
                .descuento(descuento)
                .enCombo(combo.nombre())
                .build();
    }
}
