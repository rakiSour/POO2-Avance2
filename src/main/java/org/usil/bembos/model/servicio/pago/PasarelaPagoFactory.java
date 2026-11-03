package org.usil.bembos.model.servicio.pago;

import org.usil.bembos.model.entidad.MetodoPago;

/**
 * <b>Patron Factory (creacional).</b>
 * Devuelve la pasarela adecuada segun el metodo de pago elegido en la Vista;
 * el Facade no necesita conocer las clases concretas.
 */
public final class PasarelaPagoFactory {

    private PasarelaPagoFactory() {
    }

    public static PasarelaPago crear(MetodoPago metodo) {
        if (metodo == null) {
            throw new IllegalArgumentException("Debe seleccionar un método de pago");
        }
        return switch (metodo) {
            case EFECTIVO -> new Pasarelas.Efectivo();
            case TARJETA -> new Pasarelas.Tarjeta();
            case BILLETERA_DIGITAL -> new Pasarelas.BilleteraDigital();
        };
    }
}
