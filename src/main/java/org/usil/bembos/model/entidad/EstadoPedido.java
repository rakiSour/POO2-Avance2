package org.usil.bembos.model.entidad;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de un pedido (maquina de estados).
 * <pre>
 * REGISTRADO -> PAGADO -> EN_PREPARACION -> LISTO -> ENTREGADO
 *      \__________\________ ANULADO
 * </pre>
 */
public enum EstadoPedido {
    REGISTRADO("Registrado"),
    PAGADO("Pagado / en cola"),
    EN_PREPARACION("En preparación"),
    LISTO("Listo para entregar"),
    ENTREGADO("Entregado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoPedido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Estados a los que se puede pasar desde el estado actual. */
    public Set<EstadoPedido> siguientesPermitidos() {
        return switch (this) {
            case REGISTRADO -> EnumSet.of(PAGADO, ANULADO);
            case PAGADO -> EnumSet.of(EN_PREPARACION, ANULADO);
            case EN_PREPARACION -> EnumSet.of(LISTO);
            case LISTO -> EnumSet.of(ENTREGADO);
            case ENTREGADO, ANULADO -> EnumSet.noneOf(EstadoPedido.class);
        };
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return siguientesPermitidos().contains(destino);
    }

    /** Siguiente estado del flujo normal de cocina (sin anulacion). */
    public EstadoPedido siguienteEnFlujo() {
        return switch (this) {
            case REGISTRADO -> PAGADO;
            case PAGADO -> EN_PREPARACION;
            case EN_PREPARACION -> LISTO;
            case LISTO -> ENTREGADO;
            case ENTREGADO, ANULADO -> null;
        };
    }

    public boolean esFinal() {
        return this == ENTREGADO || this == ANULADO;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
