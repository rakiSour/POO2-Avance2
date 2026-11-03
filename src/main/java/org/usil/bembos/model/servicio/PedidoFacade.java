package org.usil.bembos.model.servicio;

import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.dao.PagoDAO;
import org.usil.bembos.model.dao.PedidoDAO;
import org.usil.bembos.model.dao.ProductoDAO;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.entidad.ResumenVentas;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.excepcion.ReglaNegocioException;
import org.usil.bembos.model.servicio.facturacion.FacturacionLegacyAdapter;
import org.usil.bembos.model.servicio.facturacion.ServicioFacturacion;
import org.usil.bembos.model.servicio.facturacion.SistemaFacturacionLegacy;
import org.usil.bembos.model.servicio.pago.PasarelaPago;
import org.usil.bembos.model.servicio.pago.PasarelaPagoFactory;
import org.usil.bembos.model.servicio.pago.ResultadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Patron Facade (estructural).</b>
 * Ofrece a los Controladores una interfaz unica y simple sobre los
 * subsistemas del Modelo: catalogo, persistencia de pedidos, pasarelas de
 * pago (Factory), facturacion heredada (Adapter) y flujo de cocina.
 * Por ejemplo, {@link #procesarPago} oculta los cinco pasos del cobro.
 */
public class PedidoFacade {

    /** Estados que se muestran en la pantalla de cocina (KDS). */
    public static final Set<EstadoPedido> ESTADOS_COCINA =
            EnumSet.of(EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);

    private final ProductoDAO productoDAO;
    private final PedidoDAO pedidoDAO;
    private final PagoDAO pagoDAO;
    private final ServicioFacturacion facturacion;
    private final DAOFactory.TipoAlmacenamiento almacenamiento;

    public PedidoFacade(DAOFactory daoFactory, ServicioFacturacion facturacion) {
        this.productoDAO = daoFactory.getProductoDAO();
        this.pedidoDAO = daoFactory.getPedidoDAO();
        this.pagoDAO = daoFactory.getPagoDAO();
        this.facturacion = facturacion;
        this.almacenamiento = daoFactory.getTipo();
    }

    /**
     * Ensambla el Facade con el adaptador de facturacion, sincronizando los
     * correlativos con los comprobantes ya registrados.
     */
    public static PedidoFacade crear(DAOFactory daoFactory) {
        SistemaFacturacionLegacy legacy = new SistemaFacturacionLegacy();
        for (TipoComprobante tipo : TipoComprobante.values()) {
            legacy.sincronizarCorrelativo(tipo.getSerie(), daoFactory.getPagoDAO().ultimoCorrelativo(tipo.getSerie()));
        }
        return new PedidoFacade(daoFactory, new FacturacionLegacyAdapter(legacy));
    }

    // ------------------------------------------------------------------ catalogo

    public List<Producto> obtenerCatalogo() {
        return productoDAO.listarActivos();
    }

    public List<Ingrediente> obtenerAdicionales() {
        return productoDAO.listarAdicionales();
    }

    // ------------------------------------------------------------------ pedidos

    public Pedido registrarPedido(Pedido pedido) {
        if (pedido.getId() != null) {
            throw new ReglaNegocioException("El pedido " + pedido.getCodigo() + " ya fue registrado.");
        }
        return pedidoDAO.guardar(pedido);
    }

    /**
     * Cobra el pedido: (1) valida el estado, (2) obtiene la pasarela con la
     * fabrica, (3) cobra, (4) emite el comprobante mediante el adaptador,
     * (5) registra pago + comprobante y envia el pedido a cocina.
     */
    public Comprobante procesarPago(Pedido pedido, MetodoPago metodo, BigDecimal montoRecibido,
                                    TipoComprobante tipo, String documento, String razonSocial) {
        if (pedido.getId() == null) {
            throw new ReglaNegocioException("Primero debe registrarse el pedido.");
        }
        if (!pedido.getEstado().puedePasarA(EstadoPedido.PAGADO)) {
            throw new ReglaNegocioException("El pedido " + pedido.getCodigo() + " no está pendiente de pago.");
        }
        PasarelaPago pasarela = PasarelaPagoFactory.crear(metodo);
        ResultadoPago resultado = pasarela.cobrar(pedido.total(), montoRecibido);
        if (!resultado.aprobado()) {
            throw new ReglaNegocioException(resultado.mensaje());
        }
        Comprobante comprobante = facturacion.emitir(pedido, tipo, documento, razonSocial);
        Pago pago = new Pago(pedido.getId(), metodo, pedido.total(), resultado.montoRecibido(),
                resultado.vuelto(), resultado.referencia(), LocalDateTime.now());
        pagoDAO.registrar(pago, comprobante);
        pedido.cambiarEstado(EstadoPedido.PAGADO);
        pedidoDAO.actualizarEstado(pedido.getId(), EstadoPedido.PAGADO);
        return comprobante;
    }

    // ------------------------------------------------------------------ cocina

    public List<ResumenPedido> pedidosEnCocina() {
        return pedidoDAO.listarPorEstados(ESTADOS_COCINA);
    }

    /** Avanza el pedido al siguiente estado del flujo de cocina. */
    public EstadoPedido avanzarEstado(long idPedido) {
        EstadoPedido actual = estadoDe(idPedido);
        if (actual == EstadoPedido.REGISTRADO) {
            throw new ReglaNegocioException("El pedido aún no ha sido pagado.");
        }
        EstadoPedido siguiente = actual.siguienteEnFlujo();
        if (siguiente == null) {
            throw new ReglaNegocioException("El pedido ya se encuentra en estado final (" + actual + ").");
        }
        pedidoDAO.actualizarEstado(idPedido, siguiente);
        return siguiente;
    }

    public void anularPedido(long idPedido) {
        EstadoPedido actual = estadoDe(idPedido);
        if (!actual.puedePasarA(EstadoPedido.ANULADO)) {
            throw new ReglaNegocioException("No se puede anular un pedido en estado " + actual + ".");
        }
        pedidoDAO.actualizarEstado(idPedido, EstadoPedido.ANULADO);
    }

    public EstadoPedido estadoDe(long idPedido) {
        return pedidoDAO.obtenerEstado(idPedido)
                .orElseThrow(() -> new ReglaNegocioException("No existe el pedido " + idPedido + "."));
    }

    // ------------------------------------------------------------------ reportes

    public ResumenVentas resumenDelDia() {
        return pedidoDAO.resumenDelDia(LocalDate.now());
    }

    public DAOFactory.TipoAlmacenamiento getAlmacenamiento() {
        return almacenamiento;
    }
}
