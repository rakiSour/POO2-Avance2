package org.usil.bembos.model.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.usil.bembos.model.builder.ItemPedidoBuilder;
import org.usil.bembos.model.builder.PedidoBuilder;
import org.usil.bembos.model.dao.memoria.CatalogoSemilla;
import org.usil.bembos.model.dao.memoria.MemoriaDAOFactory;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.ResumenVentas;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.excepcion.ReglaNegocioException;
import org.usil.bembos.model.servicio.facturacion.FacturacionLegacyAdapter;
import org.usil.bembos.model.servicio.facturacion.SistemaFacturacionLegacy;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Patrones Facade y Adapter · flujo completo del pedido")
class PedidoFacadeTest {

    private MemoriaDAOFactory daos;
    private CatalogoSemilla cat;
    private PedidoFacade facade;

    @BeforeEach
    void preparar() {
        daos = new MemoriaDAOFactory();
        cat = daos.getCatalogo();
        facade = PedidoFacade.crear(daos);
    }

    private Pedido nuevoPedido() {
        return new PedidoBuilder()
                .cliente(new Cliente("70123456", "Ana Torres"))
                .agregarItem(ItemPedidoBuilder.de(cat.producto("HAM-POB"))
                        .sin(cat.ingrediente("Lechuga")).conExtra(cat.ingrediente("Tocino")).build())
                .agregarItem(ItemPedidoBuilder.de(cat.producto("BEB-CHI")).build())
                .build();
    }

    @Test
    @DisplayName("CP-30 registrarPedido asigna id y código correlativo")
    void registrar() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        assertAll(
                () -> assertEquals(1L, p.getId()),
                () -> assertEquals("PED-000001", p.getCodigo()),
                () -> assertEquals(EstadoPedido.REGISTRADO, facade.estadoDe(p.getId())));
    }

    @Test
    @DisplayName("CP-31 procesarPago cobra, emite boleta (Adapter) y envía a cocina")
    void flujoDePago() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        // total = (17.90 + 3.50 tocino) + 6.00 chicha = 27.40 -> vuelto de 50.00 = 22.60
        Comprobante c = facade.procesarPago(p, MetodoPago.EFECTIVO, new BigDecimal("50.00"),
                TipoComprobante.BOLETA, "70123456", "Ana Torres");
        assertAll(
                () -> assertEquals("B001-00000001", c.numeroCompleto()),
                () -> assertEquals(p.total(), c.total()),
                () -> assertEquals(EstadoPedido.PAGADO, p.getEstado()),
                () -> assertEquals(EstadoPedido.PAGADO, facade.estadoDe(p.getId())),
                () -> assertEquals(1, facade.pedidosEnCocina().size()),
                () -> assertEquals(new BigDecimal("22.60"), daos.getPagoDAO().pagoDe(p.getId()).orElseThrow().vuelto()));
    }

    @Test
    @DisplayName("CP-32 La factura exige RUC de 11 dígitos (validación del sistema heredado)")
    void facturaSinRuc() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> facade.procesarPago(p,
                MetodoPago.TARJETA, null, TipoComprobante.FACTURA, "70123456", "Ana Torres"));
        assertTrue(ex.getMessage().contains("RUC"));
        assertEquals(EstadoPedido.REGISTRADO, facade.estadoDe(p.getId()));
    }

    @Test
    @DisplayName("CP-33 No se puede cobrar dos veces el mismo pedido")
    void dobleCobro() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        facade.procesarPago(p, MetodoPago.TARJETA, null, TipoComprobante.BOLETA, "", "");
        assertThrows(ReglaNegocioException.class,
                () -> facade.procesarPago(p, MetodoPago.TARJETA, null, TipoComprobante.BOLETA, "", ""));
    }

    @Test
    @DisplayName("CP-34 Flujo de cocina: Pagado → En preparación → Listo → Entregado")
    void flujoCocina() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        assertThrows(ReglaNegocioException.class, () -> facade.avanzarEstado(p.getId()));
        facade.procesarPago(p, MetodoPago.BILLETERA_DIGITAL, null, TipoComprobante.BOLETA, "", "");
        assertEquals(EstadoPedido.EN_PREPARACION, facade.avanzarEstado(p.getId()));
        assertEquals(EstadoPedido.LISTO, facade.avanzarEstado(p.getId()));
        assertEquals(EstadoPedido.ENTREGADO, facade.avanzarEstado(p.getId()));
        assertThrows(ReglaNegocioException.class, () -> facade.avanzarEstado(p.getId()));
        assertEquals(0, facade.pedidosEnCocina().size());
    }

    @Test
    @DisplayName("CP-35 Solo se anulan pedidos que aún no entran a preparación")
    void anulacion() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        facade.anularPedido(p.getId());
        assertEquals(EstadoPedido.ANULADO, facade.estadoDe(p.getId()));

        Pedido q = facade.registrarPedido(nuevoPedido());
        facade.procesarPago(q, MetodoPago.TARJETA, null, TipoComprobante.BOLETA, "", "");
        facade.avanzarEstado(q.getId());
        assertThrows(ReglaNegocioException.class, () -> facade.anularPedido(q.getId()));
    }

    @Test
    @DisplayName("CP-36 Resumen del día: ventas, personalización y tiempo de atención")
    void resumen() {
        Pedido p = facade.registrarPedido(nuevoPedido());
        facade.procesarPago(p, MetodoPago.TARJETA, null, TipoComprobante.BOLETA, "", "");
        facade.avanzarEstado(p.getId());
        facade.avanzarEstado(p.getId());
        Pedido anulado = facade.registrarPedido(nuevoPedido());
        facade.anularPedido(anulado.getId());

        ResumenVentas r = facade.resumenDelDia();
        assertAll(
                () -> assertEquals(2, r.pedidos()),
                () -> assertEquals(p.total(), r.totalVendido()),
                () -> assertEquals(50.0, r.porcentajePersonalizados(), 0.001),
                () -> assertNotNull(r.minutosPromedioAtencion()));
    }

    @Test
    @DisplayName("CP-37 Adapter traduce la trama del sistema heredado a un Comprobante")
    void adaptador() {
        SistemaFacturacionLegacy legacy = new SistemaFacturacionLegacy();
        legacy.sincronizarCorrelativo("F001", 41);
        FacturacionLegacyAdapter adapter = new FacturacionLegacyAdapter(legacy);
        Comprobante c = adapter.emitir(nuevoPedido(), TipoComprobante.FACTURA, "20100123456", "Empresa SAC");
        assertEquals("F001-00000042", c.numeroCompleto());
        assertEquals("OK;B001;1", legacy.procesarTrama("03|||10.00"));
    }
}
