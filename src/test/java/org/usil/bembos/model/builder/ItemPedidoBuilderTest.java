package org.usil.bembos.model.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usil.bembos.model.dao.memoria.CatalogoSemilla;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Patrón Builder · ItemPedidoBuilder")
class ItemPedidoBuilderTest {

    private CatalogoSemilla cat;
    private Producto royal;

    @BeforeEach
    void preparar() {
        cat = new CatalogoSemilla();
        royal = cat.producto("HAM-ROY");
    }

    @Test
    @DisplayName("CP-01 Construye una hamburguesa sin personalizar con el precio base")
    void productoSinPersonalizar() {
        ItemPedido item = ItemPedidoBuilder.de(royal).build();
        assertAll(
                () -> assertEquals(new BigDecimal("16.90"), item.precioUnitario()),
                () -> assertEquals(1, item.getCantidad()),
                () -> assertFalse(item.estaPersonalizado()),
                () -> assertEquals("1 x Hamburguesa Royal", item.descripcionComanda()));
    }

    @Test
    @DisplayName("CP-02 Ensambla paso a paso: tamaño, retiro, adicionales y nota")
    void personalizacionCompleta() {
        Ingrediente tocino = cat.ingrediente("Tocino");
        Ingrediente queso = cat.ingrediente("Queso cheddar");
        ItemPedido item = ItemPedidoBuilder.de(royal)
                .cantidad(2)
                .tamano(Tamano.AGRANDADO)
                .sin(cat.ingrediente("Tomate"))
                .conExtra(tocino)
                .conExtra(queso, 2)
                .nota("Bien cocida")
                .build();
        // 16.90 + 4.50 + 3.50 + 2 x 2.50 = 29.90 por unidad
        assertAll(
                () -> assertEquals(new BigDecimal("29.90"), item.precioUnitario()),
                () -> assertEquals(new BigDecimal("59.80"), item.subtotal()),
                () -> assertTrue(item.estaPersonalizado()),
                () -> assertEquals(3, item.getExtras().size()),
                () -> assertEquals(
                        "2 x Hamburguesa Royal (Agrandado) | SIN: Tomate | EXTRA: Tocino, Queso cheddar, Queso cheddar | Nota: Bien cocida",
                        item.descripcionComanda()));
    }

    @Test
    @DisplayName("CP-03 Rechaza quitar un ingrediente obligatorio de la receta")
    void noPermiteQuitarObligatorio() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(royal).sin(cat.ingrediente("Carne de res 1/4 lb")).build());
        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    @Test
    @DisplayName("CP-04 Rechaza quitar un ingrediente que no pertenece a la receta")
    void noPermiteQuitarAjeno() {
        assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(royal).sin(cat.ingrediente("Piña")).build());
    }

    @Test
    @DisplayName("CP-05 Rechaza personalizar un producto no personalizable (bebida)")
    void bebidaNoPersonalizable() {
        assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(cat.producto("BEB-GAS")).conExtra(cat.ingrediente("Tocino")).build());
    }

    @Test
    @DisplayName("CP-06 Limita el número de adicionales por producto")
    void limiteDeAdicionales() {
        ItemPedidoBuilder b = ItemPedidoBuilder.de(royal).conExtra(cat.ingrediente("Tocino"),
                ItemPedidoBuilder.MAX_EXTRAS + 1);
        assertThrows(ReglaNegocioException.class, b::build);
    }

    @Test
    @DisplayName("CP-07 Rechaza adicionales sin stock")
    void adicionalAgotado() {
        assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(royal).conExtra(cat.ingrediente("Champiñones")).build());
    }

    @Test
    @DisplayName("CP-08 Detecta la contradicción de quitar y agregar el mismo ingrediente")
    void contradiccion() {
        Ingrediente queso = cat.ingrediente("Queso cheddar");
        assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(royal).sin(queso).conExtra(queso).build());
    }

    @ParameterizedTest(name = "cantidad = {0}")
    @ValueSource(ints = {0, -1, 21, 100})
    @DisplayName("CP-09 Valida el rango de cantidad (1 a 20)")
    void cantidadFueraDeRango(int cantidad) {
        assertThrows(ReglaNegocioException.class, () -> ItemPedidoBuilder.de(royal).cantidad(cantidad).build());
    }

    @Test
    @DisplayName("CP-10 Reúne todos los errores en una sola excepción")
    void acumulaErrores() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> ItemPedidoBuilder.de(royal)
                .cantidad(0)
                .sin(cat.ingrediente("Pan de hamburguesa"))
                .conExtra(cat.ingrediente("Champiñones"))
                .build());
        assertEquals(3, ex.getErrores().size());
    }

    @Test
    @DisplayName("CP-11 Un postre no admite tamaño agrandado")
    void postreSinAgrandado() {
        assertThrows(ReglaNegocioException.class,
                () -> ItemPedidoBuilder.de(cat.producto("POS-HEL")).tamano(Tamano.AGRANDADO).build());
    }

    @Test
    @DisplayName("CP-12 El objeto construido es inmutable")
    void inmutable() {
        ItemPedido item = ItemPedidoBuilder.de(royal).conExtra(cat.ingrediente("Tocino")).build();
        assertThrows(UnsupportedOperationException.class, () -> item.getExtras().add(cat.ingrediente("Jamón")));
    }
}
