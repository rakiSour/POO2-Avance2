package org.usil.bembos.model.servicio.pago;

import java.math.BigDecimal;

public record ResultadoPago(boolean aprobado, String referencia, BigDecimal montoRecibido,
                            BigDecimal vuelto, String mensaje) {

    public static ResultadoPago rechazado(String mensaje) {
        return new ResultadoPago(false, "", BigDecimal.ZERO, BigDecimal.ZERO, mensaje);
    }
}
