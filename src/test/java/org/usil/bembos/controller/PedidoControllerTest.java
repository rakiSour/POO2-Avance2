package org.usil.bembos.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.usil.bembos.model.dao.memoria.CatalogoSemilla;
import org.usil.bembos.model.dao.memoria.MemoriaDAOFactory;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.entidad.TipoProducto;
import org.usil.bembos.model.excepcion.ReglaNegocioException;
import org.usil.bembos.model.servicio.PedidoFacade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Integración Controlador ↔ Modelo (MVC)")
class PedidoControllerTest {

    private CatalogoSemilla cat;
    private PedidoController pedidos;
    private PagoController pagos;
    private CocinaController cocina;

    @BeforeEach
    void preparar() {
        MemoriaDAOFactory daos = new MemoriaDAOFactory();
        cat = daos.getCatalogo();
        PedidoFacade facade = PedidoFacade.crear(daos);
        pedidos = new PedidoController(facade);
        pagos = new PagoController(facade);
        cocina = new CocinaController(facade);
    }

    @Test
    @DisplayName("CP-38 El controlador filtra el catálogo por familia")
    void filtroCatalogo() {
        assertEquals(7, pedidos.productosPorTipo(TipoProducto.HAMBURGUESA).size());
        assertEquals(16, pedidos.productosPorTipo(null).size());
    }

    @Test
    @DisplayName("CP-39 Escenario de caja completo: personalizar, combo, confirmar, cobrar y cocinar")
    void escenarioCompleto() {
        pedidos.agregarProducto(cat.producto("HAM-PAR"), 1, Tamano.AGRANDADO,
                List.of(cat.ingrediente("Cebolla")), Map.of(cat.ingrediente("Huevo frito"), 1), "Sin sal");
        pedidos.agregarCombo("Combo Crispy", Tamano.REGULAR);
        assertEquals(4, pedidos.getItems().size());

        Pedido p = pedidos.confirmarPedido("", "", CanalVenta.KIOSCO, "");
        assertTrue(pedidos.getItems().isEmpty(), "el carrito se reinicia tras confirmar");

        Comprobante c = pagos.cobrar(p, MetodoPago.EFECTIVO, new BigDecimal("100"), TipoComprobante.BOLETA, "", "");
        assertEquals(TipoComprobante.BOLETA, c.tipo());
        long id = cocina.comandasPendientes().getFirst().id();
        assertEquals(EstadoPedido.EN_PREPARACION, cocina.avanzar(id));
    }

    @Test
    @DisplayName("CP-40 Una personalización inválida no llega al carrito")
    void personalizacionInvalidaNoSeAgrega() {
        assertThrows(ReglaNegocioException.class, () -> pedidos.agregarProducto(cat.producto("HAM-CLA"), 1,
                Tamano.REGULAR, List.of(cat.ingrediente("Pan de hamburguesa")), Map.of(), ""));
        assertTrue(pedidos.getItems().isEmpty());
    }

    @Test
    @DisplayName("CP-41 Previsualización de precio en la vista de personalización")
    void previsualizacion() {
        BigDecimal precio = pedidos.previsualizarPrecio(cat.producto("HAM-CLA"), Tamano.AGRANDADO,
                Map.of(cat.ingrediente("Queso cheddar"), 2));
        assertEquals(new BigDecimal("22.40"), precio);
    }
}
