package org.usil.bembos.rendimiento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.usil.bembos.model.builder.ItemPedidoBuilder;
import org.usil.bembos.model.builder.PedidoBuilder;
import org.usil.bembos.model.dao.memoria.MemoriaDAOFactory;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;
import org.usil.bembos.model.servicio.PedidoFacade;
import org.usil.bembos.soporte.Metricas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OE1 y OE2: simulacion de 1 000 solicitudes de personalizacion generadas
 * aleatoriamente (semilla fija = reproducible). Un "oraculo" independiente,
 * escrito a partir de las reglas del negocio, decide si cada solicitud es
 * valida. Se compara:
 * <ul>
 *   <li>Modelo de referencia sin Builder: acepta cualquier combinacion
 *       (las invalidas llegan a cocina como error de personalizacion).</li>
 *   <li>Modelo con Builder: valida antes de crear el objeto.</li>
 * </ul>
 * Ademas se mide el tiempo de construccion y registro de cada pedido.
 */
@DisplayName("OE1/OE2 · Simulación de 1 000 solicitudes de personalización")
class SimulacionPersonalizacionTest {

    static final int SOLICITUDES = 1000;
    static final long SEMILLA = 2026L;

    /** Solicitud de personalizacion tal como la captura la caja. */
    record Solicitud(Producto producto, int cantidad, Tamano tamano, List<Ingrediente> quitar, List<Ingrediente> extras) {
    }

    @Test
    @DisplayName("CP-47 El Builder bloquea el 100 % de personalizaciones inválidas (vs. modelo sin Builder)")
    void simulacion() {
        Metricas.iniciar("oe1_oe2_simulacion");
        MemoriaDAOFactory daos = new MemoriaDAOFactory();
        PedidoFacade facade = PedidoFacade.crear(daos);
        List<Producto> catalogo = facade.obtenerCatalogo();
        List<Hamburguesa> hamburguesas = catalogo.stream()
                .filter(p -> p instanceof Hamburguesa).map(p -> (Hamburguesa) p).toList();
        List<Ingrediente> adicionales = facade.obtenerAdicionales();
        List<Ingrediente> todos = new ArrayList<>(daos.getCatalogo().getAdicionales());
        hamburguesas.forEach(h -> h.getReceta().keySet().forEach(i -> { if (!todos.contains(i)) todos.add(i); }));

        Random rnd = new Random(SEMILLA);
        List<Solicitud> solicitudes = new ArrayList<>();
        for (int i = 0; i < SOLICITUDES; i++) {
            Hamburguesa h = hamburguesas.get(rnd.nextInt(hamburguesas.size()));
            List<Ingrediente> receta = new ArrayList<>(h.getReceta().keySet());
            List<Ingrediente> quitar = new ArrayList<>();
            for (Ingrediente ing : receta) {
                double p = h.esObligatorio(ing) ? 0.03 : 0.25;   // a veces se intenta quitar algo obligatorio
                if (rnd.nextDouble() < p) quitar.add(ing);
            }
            if (rnd.nextDouble() < 0.03) quitar.add(todos.get(rnd.nextInt(todos.size())));   // ingrediente ajeno
            List<Ingrediente> extras = new ArrayList<>();
            int nExtras = rnd.nextDouble() < 0.04 ? 7 : rnd.nextInt(4);                        // a veces excede
            for (int k = 0; k < nExtras; k++) extras.add(adicionales.get(rnd.nextInt(adicionales.size())));
            int cantidad = rnd.nextDouble() < 0.02 ? 21 + rnd.nextInt(5) : 1 + rnd.nextInt(4);
            solicitudes.add(new Solicitud(h, cantidad, rnd.nextBoolean() ? Tamano.REGULAR : Tamano.AGRANDADO,
                    quitar, extras));
        }

        int invalidasSegunOraculo = 0;
        int erroresSinBuilder = 0;
        int erroresConBuilder = 0;
        int rechazosCorrectos = 0;
        int rechazosIndebidos = 0;
        long nanosConstruccion = 0;
        int construidas = 0;

        for (Solicitud s : solicitudes) {
            boolean valida = oraculo(s);
            if (!valida) {
                invalidasSegunOraculo++;
                erroresSinBuilder++;   // el modelo sin Builder la acepta y la envia a cocina
            }
            long t0 = System.nanoTime();
            try {
                ItemPedidoBuilder b = ItemPedidoBuilder.de(s.producto()).cantidad(s.cantidad()).tamano(s.tamano());
                s.quitar().forEach(b::sin);
                s.extras().forEach(b::conExtra);
                ItemPedido item = b.build();
                facade.registrarPedido(new PedidoBuilder().agregarItem(item).build());
                nanosConstruccion += System.nanoTime() - t0;
                construidas++;
                if (!valida) erroresConBuilder++;   // una invalida se habria colado
            } catch (ReglaNegocioException e) {
                if (valida) rechazosIndebidos++; else rechazosCorrectos++;
            }
        }

        double tasaSin = 100.0 * erroresSinBuilder / SOLICITUDES;
        double tasaCon = 100.0 * erroresConBuilder / SOLICITUDES;
        double microsPromedio = construidas == 0 ? 0 : nanosConstruccion / 1000.0 / construidas;
        String a = "oe1_oe2_simulacion";
        Metricas.registrar(a, "solicitudes", SOLICITUDES);
        Metricas.registrar(a, "semilla", SEMILLA);
        Metricas.registrar(a, "invalidas_segun_oraculo", invalidasSegunOraculo);
        Metricas.registrar(a, "tasa_error_sin_builder_pct", String.format(Locale.ROOT, "%.1f", tasaSin));
        Metricas.registrar(a, "tasa_error_con_builder_pct", String.format(Locale.ROOT, "%.1f", tasaCon));
        Metricas.registrar(a, "rechazos_correctos", rechazosCorrectos);
        Metricas.registrar(a, "rechazos_indebidos", rechazosIndebidos);
        Metricas.registrar(a, "pedidos_registrados", construidas);
        Metricas.registrar(a, "tiempo_promedio_construccion_registro_us", String.format(Locale.ROOT, "%.1f", microsPromedio));

        assertEquals(0, erroresConBuilder, "Ninguna personalización inválida debe llegar a cocina");
        assertEquals(0, rechazosIndebidos, "El Builder no debe rechazar solicitudes válidas");
        assertEquals(invalidasSegunOraculo, rechazosCorrectos);
        assertTrue(tasaCon <= 5.0, "Meta OE2: errores de personalización <= 5 %");
    }

    /** Reglas del negocio escritas de forma independiente al Builder. */
    private static boolean oraculo(Solicitud s) {
        if (s.cantidad() < 1 || s.cantidad() > 20) return false;
        if (s.extras().size() > 6) return false;
        Hamburguesa h = (Hamburguesa) s.producto();
        Set<Ingrediente> quitados = new HashSet<>(s.quitar());
        for (Ingrediente q : quitados) {
            if (!h.getReceta().containsKey(q) || h.getReceta().get(q)) return false;
        }
        for (Ingrediente e : s.extras()) {
            if (!e.disponible() || quitados.contains(e)) return false;
        }
        return true;
    }
}
