package org.usil.bembos.controller;

import org.usil.bembos.model.entidad.ResumenVentas;
import org.usil.bembos.model.servicio.PedidoFacade;

/** <b>Controlador (MVC)</b> de indicadores del dia. */
public class ReporteController {

    private final PedidoFacade facade;

    public ReporteController(PedidoFacade facade) {
        this.facade = facade;
    }

    public ResumenVentas resumenDelDia() {
        return facade.resumenDelDia();
    }

    public String origenDatos() {
        return facade.getAlmacenamiento().name();
    }
}
