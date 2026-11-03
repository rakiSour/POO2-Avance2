package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Vista resumida de un pedido para la pantalla de cocina (KDS).
 */
public record ResumenPedido(long id, String codigo, EstadoPedido estado, CanalVenta canal,
                            String cliente, LocalDateTime fechaRegistro, BigDecimal total, String detalle) {

    public long minutosTranscurridos() {
        return Duration.between(fechaRegistro, LocalDateTime.now()).toMinutes();
    }
}
