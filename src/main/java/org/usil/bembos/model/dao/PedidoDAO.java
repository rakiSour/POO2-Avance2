package org.usil.bembos.model.dao;

import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.entidad.ResumenVentas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PedidoDAO {

    /** Persiste el pedido con sus lineas y personalizaciones; asigna id y codigo. */
    Pedido guardar(Pedido pedido);

    Optional<EstadoPedido> obtenerEstado(long idPedido);

    /** Actualiza el estado y deja constancia en el historial (para medir tiempos). */
    void actualizarEstado(long idPedido, EstadoPedido nuevoEstado);

    List<ResumenPedido> listarPorEstados(Set<EstadoPedido> estados);

    ResumenVentas resumenDelDia(LocalDate fecha);

    int contar();
}
