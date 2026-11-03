package org.usil.bembos.model.dao.memoria;

import org.usil.bembos.model.dao.PedidoDAO;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.entidad.ResumenVentas;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/** Implementacion en memoria y segura entre hilos de {@link PedidoDAO}. */
public class MemoriaPedidoDAO implements PedidoDAO {

    private static final class Registro {
        final Pedido pedido;
        volatile EstadoPedido estado = EstadoPedido.REGISTRADO;
        final Map<EstadoPedido, LocalDateTime> historial = new ConcurrentHashMap<>();

        Registro(Pedido pedido) {
            this.pedido = pedido;
            historial.put(EstadoPedido.REGISTRADO, pedido.getFechaRegistro());
        }
    }

    private final Map<Long, Registro> registros = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public Pedido guardar(Pedido pedido) {
        long id = secuencia.incrementAndGet();
        pedido.asignarIdentidad(id, String.format("PED-%06d", id));
        registros.put(id, new Registro(pedido));
        return pedido;
    }

    @Override
    public Optional<EstadoPedido> obtenerEstado(long idPedido) {
        Registro r = registros.get(idPedido);
        return r == null ? Optional.empty() : Optional.of(r.estado);
    }

    @Override
    public void actualizarEstado(long idPedido, EstadoPedido nuevoEstado) {
        Registro r = registros.get(idPedido);
        if (r == null) {
            throw new IllegalArgumentException("Pedido inexistente: " + idPedido);
        }
        synchronized (r) {
            r.estado = nuevoEstado;
            r.historial.put(nuevoEstado, LocalDateTime.now());
        }
    }

    @Override
    public List<ResumenPedido> listarPorEstados(Set<EstadoPedido> estados) {
        return registros.entrySet().stream()
                .filter(e -> estados.contains(e.getValue().estado))
                .map(e -> resumen(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ResumenPedido::fechaRegistro).thenComparing(ResumenPedido::id))
                .toList();
    }

    private static ResumenPedido resumen(long id, Registro r) {
        Pedido p = r.pedido;
        String detalle = p.getItems().stream().map(ItemPedido::descripcionComanda)
                .collect(Collectors.joining("\n"));
        return new ResumenPedido(id, p.getCodigo(), r.estado, p.getCanal(), p.getCliente().nombre(),
                p.getFechaRegistro(), p.total(), detalle);
    }

    @Override
    public ResumenVentas resumenDelDia(LocalDate fecha) {
        List<Registro> delDia = registros.values().stream()
                .filter(r -> r.pedido.getFechaRegistro().toLocalDate().equals(fecha))
                .toList();
        BigDecimal vendido = delDia.stream()
                .filter(r -> r.estado != EstadoPedido.ANULADO)
                .map(r -> r.pedido.total())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<EstadoPedido, Integer> porEstado = new EnumMap<>(EstadoPedido.class);
        delDia.forEach(r -> porEstado.merge(r.estado, 1, Integer::sum));
        int items = delDia.stream().mapToInt(r -> r.pedido.getItems().size()).sum();
        int personalizados = (int) delDia.stream().flatMap(r -> r.pedido.getItems().stream())
                .filter(ItemPedido::estaPersonalizado).count();
        OptionalDouble minutos = delDia.stream()
                .filter(r -> r.historial.containsKey(EstadoPedido.LISTO))
                .mapToDouble(r -> Duration.between(r.historial.get(EstadoPedido.REGISTRADO),
                        r.historial.get(EstadoPedido.LISTO)).toMillis() / 60000.0)
                .average();
        return new ResumenVentas(fecha, delDia.size(), vendido, porEstado, personalizados, items,
                minutos.isPresent() ? minutos.getAsDouble() : null);
    }

    @Override
    public int contar() {
        return registros.size();
    }
}
