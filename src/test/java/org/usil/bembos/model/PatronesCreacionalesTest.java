package org.usil.bembos.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.TipoProducto;
import org.usil.bembos.model.factory.ProductoFactory;
import org.usil.bembos.model.servicio.pago.PasarelaPago;
import org.usil.bembos.model.servicio.pago.PasarelaPagoFactory;
import org.usil.bembos.model.servicio.pago.ResultadoPago;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Patrones Factory, Singleton y máquina de estados")
class PatronesCreacionalesTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(TipoProducto.class)
    @DisplayName("CP-21 ProductoFactory crea la subclase correcta para cada tipo")
    void fabricaDeProductos(TipoProducto tipo) {
        Producto p = ProductoFactory.crear(tipo.name(), 1, "X-" + tipo, "Prueba", "", new BigDecimal("1.00"));
        assertEquals(tipo, p.getTipo());
        assertEquals(tipo == TipoProducto.HAMBURGUESA, p instanceof Hamburguesa);
    }

    @Test
    @DisplayName("CP-22 ProductoFactory rechaza tipos desconocidos")
    void tipoDesconocido() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductoFactory.crear("PIZZA", 1, "X", "Pizza", "", BigDecimal.ONE));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(MetodoPago.class)
    @DisplayName("CP-23 PasarelaPagoFactory entrega la pasarela del método elegido")
    void fabricaDePasarelas(MetodoPago metodo) {
        PasarelaPago pasarela = PasarelaPagoFactory.crear(metodo);
        assertEquals(metodo, pasarela.getMetodo());
        assertTrue(pasarela.cobrar(new BigDecimal("20.00"), new BigDecimal("50.00")).aprobado());
    }

    @Test
    @DisplayName("CP-24 Efectivo calcula el vuelto y rechaza montos insuficientes")
    void pagoEfectivo() {
        PasarelaPago efectivo = PasarelaPagoFactory.crear(MetodoPago.EFECTIVO);
        ResultadoPago ok = efectivo.cobrar(new BigDecimal("21.50"), new BigDecimal("50.00"));
        assertEquals(new BigDecimal("28.50"), ok.vuelto());
        assertFalse(efectivo.cobrar(new BigDecimal("21.50"), new BigDecimal("20.00")).aprobado());
    }

    @Test
    @DisplayName("CP-25 Billetera digital respeta el tope de S/ 500")
    void topeBilletera() {
        assertFalse(PasarelaPagoFactory.crear(MetodoPago.BILLETERA_DIGITAL)
                .cobrar(new BigDecimal("500.01"), null).aprobado());
    }

    @Test
    @DisplayName("CP-26 DAOFactory (Abstract Factory) entrega la familia en memoria")
    void fabricaAbstracta() {
        DAOFactory f = DAOFactory.crear(DAOFactory.TipoAlmacenamiento.MEMORIA);
        assertEquals(DAOFactory.TipoAlmacenamiento.MEMORIA, f.getTipo());
        assertEquals(16, f.getProductoDAO().listarActivos().size());
    }

    @Test
    @DisplayName("CP-27 ConexionBD es Singleton incluso con 50 hilos concurrentes")
    void singletonEntreHilos() throws InterruptedException {
        Set<ConexionBD> instancias = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 50; i++) {
            pool.submit(() -> instancias.add(ConexionBD.getInstancia()));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, instancias.size());
        assertSame(ConexionBD.getInstancia(), instancias.iterator().next());
    }

    @ParameterizedTest(name = "{0} -> {1} permitido={2}")
    @CsvSource({
            "REGISTRADO, PAGADO, true",
            "REGISTRADO, EN_PREPARACION, false",
            "PAGADO, EN_PREPARACION, true",
            "EN_PREPARACION, LISTO, true",
            "LISTO, ENTREGADO, true",
            "EN_PREPARACION, ANULADO, false",
            "ENTREGADO, ANULADO, false"
    })
    @DisplayName("CP-28 Máquina de estados del pedido")
    void transiciones(EstadoPedido origen, EstadoPedido destino, boolean permitido) {
        assertEquals(permitido, origen.puedePasarA(destino));
    }

    @Test
    @DisplayName("CP-29 Instancia directa: los tipos concretos se crean vía fábrica")
    void tipoConcreto() {
        assertInstanceOf(Hamburguesa.class,
                ProductoFactory.crear(TipoProducto.HAMBURGUESA, 9, "H", "H", "", BigDecimal.TEN));
    }
}
