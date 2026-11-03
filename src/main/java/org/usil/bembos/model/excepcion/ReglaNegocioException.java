package org.usil.bembos.model.excepcion;

import java.util.List;

/**
 * Se lanza cuando una operacion viola una regla del negocio
 * (personalizacion invalida, transicion de estado no permitida, pago rechazado...).
 * Agrupa todos los errores detectados para mostrarlos juntos en la Vista.
 */
public class ReglaNegocioException extends RuntimeException {

    private final List<String> errores;

    public ReglaNegocioException(String mensaje) {
        this(List.of(mensaje));
    }

    public ReglaNegocioException(List<String> errores) {
        super(String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
