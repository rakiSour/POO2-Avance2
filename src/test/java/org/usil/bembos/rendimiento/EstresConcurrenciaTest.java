package org.usil.bembos.rendimiento;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.usil.bembos.model.builder.ItemPedidoBuilder;
import org.usil.bembos.model.builder.PedidoBuilder;
import org.usil.bembos.model.builder.PedidoDirector;
import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.servicio.PedidoFacade;
import org.usil.bembos.soporte.Metricas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OE4: prueba de estres algoritmico. Se lanzan 100 peticiones simultaneas
 * (pedido personalizado + combo + pago + paso a cocina) que parten al mismo
 * tiempo gracias a un {@link CountDownLatch}. Se verifica que no haya caidas,
 * perdidas de informacion ni codigos/comprobantes duplicados.
 */
@DisplayName("OE4 · Estrés: 100 peticiones simultáneas")
class EstresConcurrenciaTest {

    static final int PETICIONES = 100;
    static final long LIMITE_MS = 30_000;
    private static final List<Long> creadosEnBD = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void iniciarMetricas() {
        Metricas.iniciar("oe4_estres");
    }

    @AfterAll
    static void limpiarBD() throws Exception {
        if (!creadosEnBD.isEmpty()) {
            org.usil.bembos.rendimiento.LimpiezaRendimiento.eliminar(creadosEnBD);
        }
    }

    @Test
    @DisplayName("CP-45 100 pedidos concurrentes en memoria sin errores ni duplicados")
    void estresEnMemoria() throws Exception {
        ResultadoEstres r = ejecutar(DAOFactory.crear(DAOFactory.TipoAlmacenamiento.MEMORIA), "memoria", null);
        verificar(r);
    }

    @Test
    @EnabledIf("org.usil.bembos.soporte.BaseDatosDisponible#hayBase")
    @DisplayName("CP-46 100 pedidos concurrentes contra PostgreSQL usando el pool del Singleton")
    void estresEnPostgres() throws Exception {
        ResultadoEstres r = ejecutar(DAOFactory.crear(DAOFactory.TipoAlmacenamiento.POSTGRESQL), "postgresql",
                creadosEnBD);
        verificar(r);
    }

    private static void verificar(ResultadoEstres r) {
        assertEquals(0, r.errores(), "No debe haber peticiones fallidas");
        assertEquals(PETICIONES, r.codigos().size(), "Códigos de pedido únicos");
        assertEquals(PETICIONES, r.comprobantes().size(), "Comprobantes únicos");
        assertTrue(r.milisegundos() < LIMITE_MS, "Debe completar en menos de " + LIMITE_MS + " ms");
    }

    record ResultadoEstres(int errores, Set<String> codigos, Set<String> comprobantes, long milisegundos) {
    }

    static ResultadoEstres ejecutar(DAOFactory daos, String etiqueta, List<Long> registroIds) throws Exception {
        PedidoFacade facade = PedidoFacade.crear(daos);
        List<Producto> catalogo = facade.obtenerCatalogo();
        List<Ingrediente> adicionales = facade.obtenerAdicionales().stream().filter(Ingrediente::disponible).toList();
        List<Hamburguesa> hamburguesas = catalogo.stream()
                .filter(p -> p instanceof Hamburguesa).map(p -> (Hamburguesa) p).toList();
        PedidoDirector director = new PedidoDirector(catalogo);
        List<String> combos = director.combosDisponibles();

        Set<String> codigos = ConcurrentHashMap.newKeySet();
        Set<String> comprobantes = ConcurrentHashMap.newKeySet();
        List<Long> latencias = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errores = new AtomicInteger();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(PETICIONES);
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < PETICIONES; i++) {
            final int n = i;
            tareas.add(pool.submit(() -> {
                try {
                    salida.await();
                    long t0 = System.nanoTime();
                    Hamburguesa h = hamburguesas.get(n % hamburguesas.size());
                    Ingrediente quitar = h.getReceta().entrySet().stream().filter(e -> !e.getValue())
                            .map(java.util.Map.Entry::getKey).findFirst().orElse(null);
                    List<Ingrediente> opciones = adicionales.stream().filter(ing -> !ing.equals(quitar)).toList();
                    ItemPedidoBuilder item = ItemPedidoBuilder.de(h).cantidad(1 + n % 3)
                            .tamano(n % 2 == 0 ? Tamano.REGULAR : Tamano.AGRANDADO)
                            .conExtra(opciones.get(n % opciones.size()));
                    if (quitar != null) item.sin(quitar);
                    PedidoBuilder pedido = new PedidoBuilder().agregarItem(item.build());
                    director.construirCombo(combos.get(n % combos.size()), Tamano.REGULAR, pedido);

                    Pedido p = facade.registrarPedido(pedido.build());
                    if (registroIds != null) registroIds.add(p.getId());
                    Comprobante c = facade.procesarPago(p, MetodoPago.values()[n % 3], new BigDecimal("500.00"),
                            TipoComprobante.BOLETA, "", "");
                    facade.avanzarEstado(p.getId());
                    codigos.add(p.getCodigo());
                    comprobantes.add(c.numeroCompleto());
                    latencias.add((System.nanoTime() - t0) / 1_000_000);
                } catch (Exception e) {
                    errores.incrementAndGet();
                    e.printStackTrace();
                }
            }));
        }
        long inicio = System.nanoTime();
        salida.countDown();
        for (Future<?> f : tareas) {
            f.get(LIMITE_MS, TimeUnit.MILLISECONDS);
        }
        long total = (System.nanoTime() - inicio) / 1_000_000;
        pool.shutdown();

        List<Long> orden = new ArrayList<>(latencias);
        Collections.sort(orden);
        double promedio = orden.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = orden.isEmpty() ? 0 : orden.get((int) Math.ceil(orden.size() * 0.95) - 1);
        long max = orden.isEmpty() ? 0 : orden.getLast();
        double rendimiento = total == 0 ? PETICIONES : PETICIONES * 1000.0 / total;

        String a = "oe4_estres";
        Metricas.registrar(a, etiqueta + ".peticiones", PETICIONES);
        Metricas.registrar(a, etiqueta + ".exitosas", PETICIONES - errores.get());
        Metricas.registrar(a, etiqueta + ".errores", errores.get());
        Metricas.registrar(a, etiqueta + ".tiempo_total_ms", total);
        Metricas.registrar(a, etiqueta + ".latencia_promedio_ms", String.format(Locale.ROOT, "%.1f", promedio));
        Metricas.registrar(a, etiqueta + ".latencia_p95_ms", p95);
        Metricas.registrar(a, etiqueta + ".latencia_max_ms", max);
        Metricas.registrar(a, etiqueta + ".pedidos_por_segundo", String.format(Locale.ROOT, "%.1f", rendimiento));
        return new ResultadoEstres(errores.get(), codigos, comprobantes, total);
    }
}
