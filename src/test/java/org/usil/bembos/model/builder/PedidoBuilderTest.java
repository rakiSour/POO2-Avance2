package org.usil.bembos.model.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.usil.bembos.model.dao.memoria.CatalogoSemilla;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Patrón Builder · PedidoBuilder y Director")
class PedidoBuilderTest {

    private CatalogoSemilla cat;
    private PedidoDirector director;

    @BeforeEach
    void preparar() {
        cat = new CatalogoSemilla();
        director = new PedidoDirector(cat.getProductos());
    }

    @Test
    @DisplayName("CP-13 Calcula total, base imponible e IGV (precios incluyen IGV)")
    void totalesConIgv() {
        Pedido p = new PedidoBuilder()
                .agregarItem(ItemPedidoBuilder.de(cat.producto("HAM-CLA")).build())   // 12.90
                .agregarItem(ItemPedidoBuilder.de(cat.producto("COM-PAP")).build())   //  6.90
                .agregarItem(ItemPedidoBuilder.de(cat.producto("BEB-GAS")).build())   //  5.50
                .build();
        assertAll(
                () -> assertEquals(new BigDecimal("25.30"), p.total()),
                () -> assertEquals(new BigDecimal("21.44"), p.baseImponible()),
                () -> assertEquals(new BigDecimal("3.86"), p.igv()),
                () -> assertEquals(EstadoPedido.REGISTRADO, p.getEstado()),
                () -> assertEquals(3, p.cantidadArticulos()));
    }

    @Test
    @DisplayName("CP-14 No permite confirmar un pedido vacío")
    void pedidoVacio() {
        assertThrows(ReglaNegocioException.class, () -> new PedidoBuilder().build());
    }

    @Test
    @DisplayName("CP-15 Delivery exige identificar al cliente")
    void deliverySinCliente() {
        PedidoBuilder b = new PedidoBuilder().canal(CanalVenta.DELIVERY)
                .agregarItem(ItemPedidoBuilder.de(cat.producto("HAM-CLA")).build());
        assertThrows(ReglaNegocioException.class, b::build);
        b.cliente(new Cliente("70123456", "Ana Torres"));
        assertEquals(CanalVenta.DELIVERY, b.build().getCanal());
    }

    @Test
    @DisplayName("CP-16 Limita el pedido a 50 artículos")
    void limiteArticulos() {
        PedidoBuilder b = new PedidoBuilder();
        for (int i = 0; i < 3; i++) {
            b.agregarItem(ItemPedidoBuilder.de(cat.producto("HAM-CLA")).cantidad(20).build());
        }
        assertThrows(ReglaNegocioException.class, b::build);
    }

    @Test
    @DisplayName("CP-17 El Director construye el Combo Clásico con 15 % de descuento")
    void comboClasico() {
        PedidoBuilder b = new PedidoBuilder();
        List<ItemPedido> lineas = director.construirCombo("Combo Clásico", Tamano.REGULAR, b);
        // 12.90 + 6.90 + 5.50 = 25.30 ; descuentos 1.94 + 1.04 + 0.83 = 3.81 (15 % por línea, redondeo HALF_UP)
        BigDecimal total = b.build().total();
        assertAll(
                () -> assertEquals(3, lineas.size()),
                () -> assertTrue(lineas.stream().allMatch(l -> "Combo Clásico".equals(l.getEtiquetaCombo()))),
                () -> assertEquals(new BigDecimal("21.49"), total));
    }

    @Test
    @DisplayName("CP-18 El Director agrupa unidades repetidas (Combo Dúo)")
    void comboDuo() {
        PedidoBuilder b = new PedidoBuilder();
        List<ItemPedido> lineas = director.construirCombo("Combo Dúo", Tamano.AGRANDADO, b);
        assertEquals(4, lineas.size());
        assertEquals(6, b.build().cantidadArticulos());
    }

    @Test
    @DisplayName("CP-19 El Director rechaza combos inexistentes")
    void comboInexistente() {
        assertThrows(ReglaNegocioException.class,
                () -> director.construirCombo("Combo Fantasma", Tamano.REGULAR, new PedidoBuilder()));
    }

    @Test
    @DisplayName("CP-20 Quitar un ítem con índice inválido se informa como regla de negocio")
    void quitarIndiceInvalido() {
        assertThrows(ReglaNegocioException.class, () -> new PedidoBuilder().quitarItem(0));
    }
}
