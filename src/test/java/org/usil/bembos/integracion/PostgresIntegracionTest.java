package org.usil.bembos.integracion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.builder.ItemPedidoBuilder;
import org.usil.bembos.model.builder.PedidoBuilder;
import org.usil.bembos.model.builder.PedidoDirector;
import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.servicio.PedidoFacade;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion contra PostgreSQL (esquema bembos).
 * Requieren haber ejecutado db/01_schema.sql y db/02_datos.sql.
 * Los pedidos creados se eliminan al finalizar.
 */
@EnabledIf("org.usil.bembos.soporte.BaseDatosDisponible#hayBase")
@DisplayName("Integración con PostgreSQL (JDBC + Singleton)")
class PostgresIntegracionTest {

    private static PedidoFacade facade;
    private static Map<String, Producto> productos;
    private static final List<Long> creados = new ArrayList<>();

    @BeforeAll
    static void preparar() {
        facade = PedidoFacade.crear(DAOFactory.crear(DAOFactory.TipoAlmacenamiento.POSTGRESQL));
        productos = facade.obtenerCatalogo().stream()
                .collect(Collectors.toMap(Producto::getCodigo, Function.identity()));
    }

    @AfterAll
    static void limpiar() throws Exception {
        LimpiezaBD.eliminarPedidos(creados);
    }

    private static Ingrediente ingrediente(Hamburguesa h, String nombre) {
        return h.getReceta().keySet().stream().filter(i -> i.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("CP-42 El catálogo se carga desde la BD mediante ProductoFactory, con recetas")
    void catalogo() {
        assertEquals(16, productos.size());
        Hamburguesa royal = (Hamburguesa) productos.get("HAM-ROY");
        assertEquals(7, royal.getReceta().size());
        assertTrue(royal.esObligatorio(ingrediente(royal, "Huevo frito")));
        assertFalse(facade.obtenerAdicionales().isEmpty());
    }

    @Test
    @DisplayName("CP-43 Persiste pedido, personalizaciones, pago, comprobante e historial")
    void persistenciaCompleta() throws Exception {
        Hamburguesa royal = (Hamburguesa) productos.get("HAM-ROY");
        Ingrediente tocino = facade.obtenerAdicionales().stream()
                .filter(i -> i.nombre().equals("Tocino")).findFirst().orElseThrow();
        PedidoBuilder b = new PedidoBuilder()
                .cliente(new Cliente("70999888", "Cliente Integración"))
                .canal(CanalVenta.DELIVERY)
                .agregarItem(ItemPedidoBuilder.de(royal).sin(ingrediente(royal, "Tomate"))
                        .conExtra(tocino, 2).tamano(Tamano.AGRANDADO).build());
        new PedidoDirector(new ArrayList<>(productos.values())).construirCombo("Combo Clásico", Tamano.REGULAR, b);
        Pedido p = facade.registrarPedido(b.build());
        creados.add(p.getId());
        assertTrue(p.getCodigo().startsWith("PED-"));

        Comprobante c = facade.procesarPago(p, MetodoPago.EFECTIVO, new BigDecimal("100.00"),
                TipoComprobante.BOLETA, "70999888", "Cliente Integración");
        facade.avanzarEstado(p.getId());
        facade.avanzarEstado(p.getId());
        assertEquals(EstadoPedido.LISTO, facade.estadoDe(p.getId()));

        try (Connection cn = ConexionBD.getInstancia().obtenerConexion();
             PreparedStatement ps = cn.prepareStatement("""
                     SELECT (SELECT count(*) FROM detalle_pedido WHERE id_pedido = ?) AS detalles,
                            (SELECT count(*) FROM detalle_personalizacion x JOIN detalle_pedido d USING (id_detalle)
                              WHERE d.id_pedido = ?) AS personalizaciones,
                            (SELECT count(*) FROM historial_estado WHERE id_pedido = ?) AS historial,
                            (SELECT total FROM pedido WHERE id_pedido = ?) AS total,
                            (SELECT numero FROM comprobante WHERE id_pedido = ?) AS numero""")) {
            for (int i = 1; i <= 5; i++) ps.setLong(i, p.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(4, rs.getInt("detalles"));
                assertEquals(3, rs.getInt("personalizaciones"));     // 1 QUITAR + 2 AGREGAR
                assertEquals(4, rs.getInt("historial"));             // REGISTRADO, PAGADO, EN_PREP, LISTO
                assertEquals(0, p.total().compareTo(rs.getBigDecimal("total")));
                assertEquals(c.numero(), rs.getInt("numero"));
            }
        }
        assertTrue(facade.pedidosEnCocina().stream().anyMatch(r -> r.id() == p.getId()));
        assertTrue(facade.resumenDelDia().minutosPromedioAtencion() != null);
    }

    @Test
    @DisplayName("CP-48 Tiempo de registro y cobro de un pedido personalizado en PostgreSQL (50 muestras)")
    void tiempoDeRegistro() {
        org.usil.bembos.soporte.Metricas.iniciar("oe1_registro_postgres");
        Hamburguesa clasica = (Hamburguesa) productos.get("HAM-CLA");
        Ingrediente queso = facade.obtenerAdicionales().stream()
                .filter(i -> i.nombre().equals("Queso cheddar")).findFirst().orElseThrow();
        List<Double> muestras = new ArrayList<>();
        for (int i = 0; i < 55; i++) {
            long t0 = System.nanoTime();
            Pedido p = facade.registrarPedido(new PedidoBuilder()
                    .agregarItem(ItemPedidoBuilder.de(clasica).sin(ingrediente(clasica, "Lechuga"))
                            .conExtra(queso).build())
                    .agregarItem(ItemPedidoBuilder.de(productos.get("BEB-GAS")).build())
                    .build());
            facade.procesarPago(p, MetodoPago.TARJETA, null, TipoComprobante.BOLETA, "", "");
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            creados.add(p.getId());
            if (i >= 5) muestras.add(ms);   // se descartan 5 iteraciones de calentamiento
        }
        double prom = muestras.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double max = muestras.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        org.usil.bembos.soporte.Metricas.registrar("oe1_registro_postgres", "muestras", muestras.size());
        org.usil.bembos.soporte.Metricas.registrar("oe1_registro_postgres", "promedio_ms",
                String.format(java.util.Locale.ROOT, "%.2f", prom));
        org.usil.bembos.soporte.Metricas.registrar("oe1_registro_postgres", "maximo_ms",
                String.format(java.util.Locale.ROOT, "%.2f", max));
        assertTrue(prom < 1000, "El registro y cobro debe tomar menos de 1 s");
    }

    @Test
    @DisplayName("CP-44 El pool del Singleton reutiliza conexiones (no crea una por consulta)")
    void reutilizaConexiones() throws Exception {
        ConexionBD bd = ConexionBD.getInstancia();
        for (int i = 0; i < 200; i++) {
            try (Connection cn = bd.obtenerConexion()) {
                assertTrue(cn.isValid(1));
            }
        }
        assertTrue(bd.conexionesCreadas() <= bd.getTamanoMaximo());
    }
}
