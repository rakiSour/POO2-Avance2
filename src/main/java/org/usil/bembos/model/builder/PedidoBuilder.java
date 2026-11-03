package org.usil.bembos.model.builder;

import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>Patron Builder (creacional).</b>
 * Construye el pedido completo de forma incremental: el cajero o el kiosco
 * agregan lineas (cada una creada por {@link ItemPedidoBuilder}) y, al
 * confirmar, {@link #build()} valida el pedido y entrega un objeto consistente.
 */
public final class PedidoBuilder {

    public static final int MAX_ARTICULOS = 50;

    private Cliente cliente = Cliente.CLIENTE_VARIOS;
    private CanalVenta canal = CanalVenta.MOSTRADOR;
    private final List<ItemPedido> items = new ArrayList<>();
    private String observacion = "";

    public PedidoBuilder cliente(Cliente cliente) {
        this.cliente = cliente == null ? Cliente.CLIENTE_VARIOS : cliente;
        return this;
    }

    public PedidoBuilder canal(CanalVenta canal) {
        this.canal = canal;
        return this;
    }

    public PedidoBuilder observacion(String observacion) {
        this.observacion = observacion == null ? "" : observacion.trim();
        return this;
    }

    public PedidoBuilder agregarItem(ItemPedido item) {
        items.add(item);
        return this;
    }

    public PedidoBuilder quitarItem(int indice) {
        if (indice < 0 || indice >= items.size()) {
            throw new ReglaNegocioException("Seleccione un ítem válido para quitar.");
        }
        items.remove(indice);
        return this;
    }

    public PedidoBuilder limpiar() {
        items.clear();
        cliente = Cliente.CLIENTE_VARIOS;
        canal = CanalVenta.MOSTRADOR;
        observacion = "";
        return this;
    }

    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** Total parcial mientras el pedido aun se esta armando. */
    public BigDecimal totalParcial() {
        return items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Pedido build() {
        List<String> errores = new ArrayList<>();
        if (items.isEmpty()) {
            errores.add("El pedido debe tener al menos un producto.");
        }
        int articulos = items.stream().mapToInt(ItemPedido::getCantidad).sum();
        if (articulos > MAX_ARTICULOS) {
            errores.add("Un pedido no puede superar " + MAX_ARTICULOS + " artículos.");
        }
        if (canal == null) {
            errores.add("Debe indicar el canal de venta.");
        }
        if (canal == CanalVenta.DELIVERY && !cliente.tieneDocumento()) {
            errores.add("Los pedidos delivery requieren identificar al cliente (DNI o RUC).");
        }
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException(errores);
        }
        return new Pedido(cliente, canal, items, observacion, LocalDateTime.now());
    }
}
