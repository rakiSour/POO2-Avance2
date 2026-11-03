package org.usil.bembos.model.dao;

import org.usil.bembos.model.dao.jdbc.JdbcDAOFactory;
import org.usil.bembos.model.dao.memoria.MemoriaDAOFactory;

/**
 * <b>Patron Abstract Factory (familia Factory).</b>
 * Crea una familia coherente de DAOs. La aplicacion trabaja con PostgreSQL
 * ({@link JdbcDAOFactory}) y, para pruebas unitarias o demostraciones sin
 * servidor, con almacenamiento en memoria ({@link MemoriaDAOFactory}).
 */
public abstract class DAOFactory {

    public enum TipoAlmacenamiento { POSTGRESQL, MEMORIA }

    public abstract ProductoDAO getProductoDAO();

    public abstract PedidoDAO getPedidoDAO();

    public abstract PagoDAO getPagoDAO();

    public abstract TipoAlmacenamiento getTipo();

    public static DAOFactory crear(TipoAlmacenamiento tipo) {
        return switch (tipo) {
            case POSTGRESQL -> new JdbcDAOFactory();
            case MEMORIA -> new MemoriaDAOFactory();
        };
    }
}
