package org.usil.bembos.controller;

import org.usil.bembos.model.builder.ItemPedidoBuilder;
import org.usil.bembos.model.builder.PedidoBuilder;
import org.usil.bembos.model.builder.PedidoDirector;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.entidad.TipoProducto;
import org.usil.bembos.model.servicio.PedidoFacade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <b>Controlador (MVC)</b> de la toma de pedidos.
 * Recibe las acciones de la Vista y delega la construccion en los builders
 * del Modelo. No contiene reglas de personalizacion: si una combinacion es
 * invalida, el Builder lanza {@code ReglaNegocioException} y la Vista la muestra.
 */
public class PedidoController {

    private final PedidoFacade facade;
    private final List<Producto> catalogo;
    private final List<Ingrediente> adicionales;
    private final PedidoDirector director;
    private PedidoBuilder pedidoEnCurso = new PedidoBuilder();

    public PedidoController(PedidoFacade facade) {
        this.facade = facade;
        this.catalogo = facade.obtenerCatalogo();
        this.adicionales = facade.obtenerAdicionales();
        this.director = new PedidoDirector(catalogo);
    }

    public List<Producto> getCatalogo() {
        return catalogo;
    }

    public List<Producto> productosPorTipo(TipoProducto tipo) {
        return tipo == null ? catalogo : catalogo.stream().filter(p -> p.getTipo() == tipo).toList();
    }

    public List<Ingrediente> getAdicionales() {
        return adicionales;
    }

    public List<String> getCombos() {
        return director.combosDisponibles();
    }

    /**
     * Agrega un producto personalizado al pedido en curso.
     *
     * @param extras ingrediente adicional -> numero de porciones
     */
    public ItemPedido agregarProducto(Producto producto, int cantidad, Tamano tamano,
                                      List<Ingrediente> quitar, Map<Ingrediente, Integer> extras, String nota) {
        ItemPedidoBuilder builder = ItemPedidoBuilder.de(producto).cantidad(cantidad).tamano(tamano).nota(nota);
        quitar.forEach(builder::sin);
        extras.forEach(builder::conExtra);
        ItemPedido item = builder.build();
        pedidoEnCurso.agregarItem(item);
        return item;
    }

    /** Precio unitario estimado mientras el usuario personaliza (sin agregar al pedido). */
    public BigDecimal previsualizarPrecio(Producto producto, Tamano tamano, Map<Ingrediente, Integer> extras) {
        BigDecimal precio = producto.getPrecioBase().add(tamano.getRecargo());
        for (Map.Entry<Ingrediente, Integer> e : extras.entrySet()) {
            precio = precio.add(e.getKey().precioExtra().multiply(BigDecimal.valueOf(e.getValue())));
        }
        return precio;
    }

    public List<ItemPedido> agregarCombo(String nombreCombo, Tamano tamano) {
        return director.construirCombo(nombreCombo, tamano, pedidoEnCurso);
    }

    public void quitarItem(int indice) {
        pedidoEnCurso.quitarItem(indice);
    }

    public List<ItemPedido> getItems() {
        return pedidoEnCurso.getItems();
    }

    public BigDecimal getTotal() {
        return pedidoEnCurso.totalParcial();
    }

    /** Valida el pedido con el Builder y lo registra (estado REGISTRADO). */
    public Pedido confirmarPedido(String documento, String nombre, CanalVenta canal, String observacion) {
        Pedido pedido = pedidoEnCurso
                .cliente(new Cliente(documento, nombre))
                .canal(canal)
                .observacion(observacion)
                .build();
        Pedido registrado = facade.registrarPedido(pedido);
        pedidoEnCurso = new PedidoBuilder();
        return registrado;
    }

    public void cancelarPedidoEnCurso() {
        pedidoEnCurso = new PedidoBuilder();
    }

    public PedidoFacade getFacade() {
        return facade;
    }
}
