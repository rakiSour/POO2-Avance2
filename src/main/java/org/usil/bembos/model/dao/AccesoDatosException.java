package org.usil.bembos.model.dao;

/**
 * Error tecnico de persistencia (envuelve SQLException) para que las capas
 * superiores no dependan de JDBC.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
