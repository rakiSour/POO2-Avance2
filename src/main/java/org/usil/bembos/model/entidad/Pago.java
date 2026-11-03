package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago registrado de un pedido.
 */
public record Pago(long idPedido, MetodoPago metodo, BigDecimal monto, BigDecimal montoRecibido,
                   BigDecimal vuelto, String referencia, LocalDateTime fecha) {
}
