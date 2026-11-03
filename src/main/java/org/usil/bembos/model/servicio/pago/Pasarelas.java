package org.usil.bembos.model.servicio.pago;

import org.usil.bembos.model.entidad.MetodoPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementaciones concretas de {@link PasarelaPago}. Las de tarjeta y
 * billetera son simulaciones (no se conectan a un banco real).
 */
final class Pasarelas {

    private static final AtomicLong CORRELATIVO = new AtomicLong(100000);

    private Pasarelas() {
    }

    static String nuevaReferencia(String prefijo) {
        return prefijo + "-" + CORRELATIVO.incrementAndGet();
    }

    /** Pago en efectivo: exige que el monto recibido cubra el total y calcula el vuelto. */
    static final class Efectivo implements PasarelaPago {
        @Override
        public MetodoPago getMetodo() {
            return MetodoPago.EFECTIVO;
        }

        @Override
        public ResultadoPago cobrar(BigDecimal total, BigDecimal montoRecibido) {
            if (montoRecibido == null || montoRecibido.compareTo(total) < 0) {
                return ResultadoPago.rechazado("El monto recibido es menor al total (S/ " + total + ").");
            }
            BigDecimal vuelto = montoRecibido.subtract(total).setScale(2, RoundingMode.HALF_UP);
            return new ResultadoPago(true, nuevaReferencia("EFE"), montoRecibido, vuelto, "Pago en efectivo registrado");
        }
    }

    /** Pago con tarjeta (POS simulado): se cobra el monto exacto. */
    static final class Tarjeta implements PasarelaPago {
        static final BigDecimal MONTO_MINIMO = new BigDecimal("1.00");

        @Override
        public MetodoPago getMetodo() {
            return MetodoPago.TARJETA;
        }

        @Override
        public ResultadoPago cobrar(BigDecimal total, BigDecimal montoRecibido) {
            if (total.compareTo(MONTO_MINIMO) < 0) {
                return ResultadoPago.rechazado("El POS no acepta montos menores a S/ 1.00.");
            }
            return new ResultadoPago(true, nuevaReferencia("POS"), total, BigDecimal.ZERO.setScale(2),
                    "Operación aprobada por el POS");
        }
    }

    /** Pago con billetera digital (QR simulado), con tope por operacion. */
    static final class BilleteraDigital implements PasarelaPago {
        static final BigDecimal TOPE = new BigDecimal("500.00");

        @Override
        public MetodoPago getMetodo() {
            return MetodoPago.BILLETERA_DIGITAL;
        }

        @Override
        public ResultadoPago cobrar(BigDecimal total, BigDecimal montoRecibido) {
            if (total.compareTo(TOPE) > 0) {
                return ResultadoPago.rechazado("Las billeteras digitales tienen un tope de S/ 500.00 por operación.");
            }
            return new ResultadoPago(true, nuevaReferencia("QR"), total, BigDecimal.ZERO.setScale(2),
                    "Pago por QR confirmado");
        }
    }
}
