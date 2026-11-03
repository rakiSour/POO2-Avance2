package org.usil.bembos.model.dao;

import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Pago;

public interface PagoDAO {

    /** Registra el pago y el comprobante en una sola transaccion. */
    void registrar(Pago pago, Comprobante comprobante);

    /** Ultimo numero emitido en la serie (0 si no hay comprobantes). */
    int ultimoCorrelativo(String serie);
}
