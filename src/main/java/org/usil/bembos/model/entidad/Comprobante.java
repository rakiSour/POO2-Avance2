package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comprobante de pago electronico emitido por el modulo de facturacion.
 */
public record Comprobante(TipoComprobante tipo, String serie, int numero, String documentoCliente,
                          String razonSocial, BigDecimal total, LocalDateTime fechaEmision) {

    public String numeroCompleto() {
        return serie + "-" + String.format("%08d", numero);
    }

    @Override
    public String toString() {
        return tipo + " " + numeroCompleto();
    }
}
