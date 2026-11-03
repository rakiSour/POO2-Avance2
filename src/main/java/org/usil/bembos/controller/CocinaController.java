package org.usil.bembos.controller;

import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.servicio.PedidoFacade;

import java.util.List;

/** <b>Controlador (MVC)</b> de la pantalla de cocina (Kitchen Display System). */
public class CocinaController {

    private final PedidoFacade facade;

    public CocinaController(PedidoFacade facade) {
        this.facade = facade;
    }

    public List<ResumenPedido> comandasPendientes() {
        return facade.pedidosEnCocina();
    }

    public EstadoPedido avanzar(long idPedido) {
        return facade.avanzarEstado(idPedido);
    }

    public void anular(long idPedido) {
        facade.anularPedido(idPedido);
    }
}
