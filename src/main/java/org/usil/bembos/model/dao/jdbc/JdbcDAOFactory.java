package org.usil.bembos.model.dao.jdbc;

import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.dao.PagoDAO;
import org.usil.bembos.model.dao.PedidoDAO;
import org.usil.bembos.model.dao.ProductoDAO;

/** Familia de DAOs que persisten en PostgreSQL usando el Singleton {@link ConexionBD}. */
public class JdbcDAOFactory extends DAOFactory {

    private final ConexionBD conexion = ConexionBD.getInstancia();
    private final ProductoDAO productoDAO = new ProductoDaoJdbc(conexion);
    private final PedidoDAO pedidoDAO = new PedidoDaoJdbc(conexion);
    private final PagoDAO pagoDAO = new PagoDaoJdbc(conexion);

    @Override
    public ProductoDAO getProductoDAO() {
        return productoDAO;
    }

    @Override
    public PedidoDAO getPedidoDAO() {
        return pedidoDAO;
    }

    @Override
    public PagoDAO getPagoDAO() {
        return pagoDAO;
    }

    @Override
    public TipoAlmacenamiento getTipo() {
        return TipoAlmacenamiento.POSTGRESQL;
    }
}
