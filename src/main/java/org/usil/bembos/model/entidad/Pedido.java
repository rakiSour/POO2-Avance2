package org.usil.bembos.model.entidad;

import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido (orden) de un cliente. Se construye unicamente con
 * {@link org.usil.bembos.model.builder.PedidoBuilder}.
 * Los precios del catalogo incluyen IGV (18 %), por lo que la base imponible
 * se obtiene dividiendo el total entre 1.18.
 */
public final class Pedido {

    public static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private Long id;
    private String codigo;
    private final Cliente cliente;
    private final CanalVenta canal;
    private final List<ItemPedido> items;
    private final String observacion;
    private final LocalDateTime fechaRegistro;
    private volatile EstadoPedido estado;

    public Pedido(Cliente cliente, CanalVenta canal, List<ItemPedido> items,
                  String observacion, LocalDateTime fechaRegistro) {
        this.cliente = cliente == null ? Cliente.CLIENTE_VARIOS : cliente;
        this.canal = canal;
        this.items = List.copyOf(items);
        this.observacion = observacion == null ? "" : observacion;
        this.fechaRegistro = fechaRegistro;
        this.estado = EstadoPedido.REGISTRADO;
    }

    /** Lo invoca la capa DAO cuando el pedido se persiste. */
    public void asignarIdentidad(long id, String codigo) {
        if (this.id != null) {
            throw new IllegalStateException("El pedido ya tiene identificador");
        }
        this.id = id;
        this.codigo = codigo;
    }

    /** Cambia de estado respetando la maquina de estados. */
    public synchronized void cambiarEstado(EstadoPedido nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new ReglaNegocioException(
                    "No se puede pasar del estado " + estado.getEtiqueta() + " a " + nuevo.getEtiqueta());
        }
        this.estado = nuevo;
    }

    public BigDecimal total() {
        return items.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal baseImponible() {
        return total().divide(BigDecimal.ONE.add(TASA_IGV), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal igv() {
        return total().subtract(baseImponible());
    }

    public BigDecimal totalDescuentos() {
        return items.stream().map(ItemPedido::getDescuento).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int cantidadArticulos() {
        return items.stream().mapToInt(ItemPedido::getCantidad).sum();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public CanalVenta getCanal() {
        return canal;
    }

    public List<ItemPedido> getItems() {
        return items;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        return (codigo == null ? "(sin registrar)" : codigo) + " - " + cliente.nombre() + " - S/ " + total();
    }
}
