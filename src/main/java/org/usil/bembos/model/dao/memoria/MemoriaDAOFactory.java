package org.usil.bembos.model.dao.memoria;

import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.dao.PagoDAO;
import org.usil.bembos.model.dao.PedidoDAO;
import org.usil.bembos.model.dao.ProductoDAO;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.Pago;
import org.usil.bembos.model.entidad.Producto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Familia de DAOs en memoria (thread-safe). Permite ejecutar las pruebas
 * unitarias y la demostracion sin depender de un servidor de base de datos.
 */
public class MemoriaDAOFactory extends DAOFactory {

    private final CatalogoSemilla catalogo = new CatalogoSemilla();
    private final MemoriaPedidoDAO pedidoDAO = new MemoriaPedidoDAO();
    private final MemoriaPagoDAO pagoDAO = new MemoriaPagoDAO();
    private final ProductoDAO productoDAO = new ProductoDAO() {
        @Override
        public List<Producto> listarActivos() {
            return catalogo.getProductos();
        }

        @Override
        public Optional<Producto> buscarPorCodigo(String codigo) {
            return catalogo.getProductos().stream().filter(p -> p.getCodigo().equalsIgnoreCase(codigo)).findFirst();
        }

        @Override
        public List<Ingrediente> listarAdicionales() {
            return catalogo.getAdicionales();
        }
    };

    public CatalogoSemilla getCatalogo() {
        return catalogo;
    }

    @Override
    public ProductoDAO getProductoDAO() {
        return productoDAO;
    }

    @Override
    public MemoriaPedidoDAO getPedidoDAO() {
        return pedidoDAO;
    }

    @Override
    public MemoriaPagoDAO getPagoDAO() {
        return pagoDAO;
    }

    @Override
    public TipoAlmacenamiento getTipo() {
        return TipoAlmacenamiento.MEMORIA;
    }

    /** DAO de pagos en memoria. */
    public static class MemoriaPagoDAO implements PagoDAO {
        private final Map<Long, Pago> pagos = new ConcurrentHashMap<>();
        private final Map<Long, Comprobante> comprobantes = new ConcurrentHashMap<>();

        @Override
        public void registrar(Pago pago, Comprobante comprobante) {
            if (pagos.putIfAbsent(pago.idPedido(), pago) != null) {
                throw new IllegalStateException("El pedido " + pago.idPedido() + " ya tiene un pago registrado");
            }
            comprobantes.put(pago.idPedido(), comprobante);
        }

        @Override
        public int ultimoCorrelativo(String serie) {
            return comprobantes.values().stream().filter(c -> c.serie().equals(serie))
                    .mapToInt(Comprobante::numero).max().orElse(0);
        }

        public Optional<Pago> pagoDe(long idPedido) {
            return Optional.ofNullable(pagos.get(idPedido));
        }

        public Optional<Comprobante> comprobanteDe(long idPedido) {
            return Optional.ofNullable(comprobantes.get(idPedido));
        }

        public int cantidad() {
            return pagos.size();
        }
    }
}
