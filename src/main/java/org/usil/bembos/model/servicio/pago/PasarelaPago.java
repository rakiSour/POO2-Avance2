package org.usil.bembos.model.servicio.pago;

import org.usil.bembos.model.entidad.MetodoPago;

import java.math.BigDecimal;

/** Producto abstracto del patron Factory: cada metodo de pago cobra a su manera. */
public interface PasarelaPago {

    MetodoPago getMetodo();

    /**
     * @param total         importe a cobrar
     * @param montoRecibido dinero entregado por el cliente (relevante en efectivo)
     */
    ResultadoPago cobrar(BigDecimal total, BigDecimal montoRecibido);
}
