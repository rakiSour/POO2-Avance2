package org.usil.bembos.model.entidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * Indicadores del dia para el modulo de reportes.
 *
 * @param minutosPromedioAtencion promedio de minutos entre REGISTRADO y LISTO
 *                                (null si aun no hay pedidos listos)
 */
public record ResumenVentas(LocalDate fecha, int pedidos, BigDecimal totalVendido,
                            Map<EstadoPedido, Integer> pedidosPorEstado,
                            int itemsPersonalizados, int itemsTotales,
                            Double minutosPromedioAtencion) {

    public BigDecimal ticketPromedio() {
        if (pedidos == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return totalVendido.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
    }

    public double porcentajePersonalizados() {
        return itemsTotales == 0 ? 0.0 : (itemsPersonalizados * 100.0) / itemsTotales;
    }
}
