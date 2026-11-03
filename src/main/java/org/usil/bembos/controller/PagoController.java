package org.usil.bembos.controller;

import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.servicio.PedidoFacade;

import java.math.BigDecimal;

/** <b>Controlador (MVC)</b> del cobro y emision de comprobantes. */
public class PagoController {

    private final PedidoFacade facade;

    public PagoController(PedidoFacade facade) {
        this.facade = facade;
    }

    public Comprobante cobrar(Pedido pedido, MetodoPago metodo, BigDecimal montoRecibido,
                              TipoComprobante tipo, String documento, String razonSocial) {
        return facade.procesarPago(pedido, metodo, montoRecibido, tipo, documento, razonSocial);
    }

    public BigDecimal calcularVuelto(BigDecimal total, BigDecimal recibido) {
        if (recibido == null || recibido.compareTo(total) < 0) {
            return BigDecimal.ZERO;
        }
        return recibido.subtract(total);
    }
}
