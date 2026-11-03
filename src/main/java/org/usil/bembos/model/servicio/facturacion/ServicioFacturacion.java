package org.usil.bembos.model.servicio.facturacion;

import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.TipoComprobante;

/**
 * Interfaz objetivo (Target) del patron Adapter: es lo que el sistema MVC
 * espera de cualquier modulo de facturacion.
 */
public interface ServicioFacturacion {

    Comprobante emitir(Pedido pedido, TipoComprobante tipo, String documentoCliente, String razonSocial);
}
