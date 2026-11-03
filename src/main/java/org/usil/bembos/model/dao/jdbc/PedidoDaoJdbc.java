package org.usil.bembos.model.dao.jdbc;

import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.dao.PedidoDAO;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Cliente;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.entidad.ResumenVentas;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PedidoDaoJdbc implements PedidoDAO {

    private static final String SQL_UPSERT_CLIENTE = """
            INSERT INTO cliente (documento, nombre) VALUES (?, ?)
            ON CONFLICT (documento) DO UPDATE SET nombre = EXCLUDED.nombre
            RETURNING id_cliente""";

    private static final String SQL_INSERT_PEDIDO = """
            INSERT INTO pedido (id_cliente, nombre_cliente, canal, estado, observacion,
                                base_imponible, igv, total, fecha_registro)
            VALUES (?, ?, ?, 'REGISTRADO', ?, ?, ?, ?, ?)
            RETURNING id_pedido, codigo""";

    private static final String SQL_INSERT_DETALLE = """
            INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, tamano, precio_unitario,
                                        descuento, subtotal, combo, nota, descripcion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String SQL_INSERT_PERSONALIZACION = """
            INSERT INTO detalle_personalizacion (id_detalle, id_ingrediente, accion, precio)
            VALUES (?, ?, ?, ?)""";

    private static final String SQL_INSERT_HISTORIAL =
            "INSERT INTO historial_estado (id_pedido, estado) VALUES (?, ?)";

    private static final String SQL_ESTADO = "SELECT estado FROM pedido WHERE id_pedido = ?";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE pedido SET estado = ?, fecha_actualizacion = now() WHERE id_pedido = ?";

    private static final String SQL_COMANDAS = """
            SELECT id_pedido, codigo, estado, canal, nombre_cliente, fecha_registro, total, detalle
            FROM v_comandas_cocina
            WHERE estado = ANY (?)
            ORDER BY fecha_registro""";

    private static final String SQL_RESUMEN = """
            SELECT count(*) AS pedidos,
                   coalesce(sum(total) FILTER (WHERE estado <> 'ANULADO'), 0) AS vendido
            FROM pedido WHERE fecha_registro::date = ?""";

    private static final String SQL_POR_ESTADO = """
            SELECT estado, count(*) FROM pedido WHERE fecha_registro::date = ? GROUP BY estado""";

    private static final String SQL_PERSONALIZADOS = """
            SELECT count(*) AS total,
                   count(*) FILTER (WHERE EXISTS (SELECT 1 FROM detalle_personalizacion x
                                                  WHERE x.id_detalle = d.id_detalle)) AS personalizados
            FROM detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
            WHERE p.fecha_registro::date = ?""";

    private static final String SQL_TIEMPO = """
            SELECT avg(minutos_hasta_listo) FROM v_tiempo_atencion WHERE fecha = ?""";

    private final ConexionBD conexionBD;

    public PedidoDaoJdbc(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        try (Connection cn = conexionBD.obtenerConexion()) {
            cn.setAutoCommit(false);
            try {
                Integer idCliente = registrarCliente(cn, pedido.getCliente());
                long idPedido;
                String codigo;
                try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_PEDIDO)) {
                    if (idCliente == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, idCliente);
                    ps.setString(2, pedido.getCliente().nombre());
                    ps.setString(3, pedido.getCanal().name());
                    ps.setString(4, pedido.getObservacion());
                    ps.setBigDecimal(5, pedido.baseImponible());
                    ps.setBigDecimal(6, pedido.igv());
                    ps.setBigDecimal(7, pedido.total());
                    ps.setTimestamp(8, Timestamp.valueOf(pedido.getFechaRegistro()));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idPedido = rs.getLong(1);
                        codigo = rs.getString(2);
                    }
                }
                for (ItemPedido item : pedido.getItems()) {
                    insertarDetalle(cn, idPedido, item);
                }
                insertarHistorial(cn, idPedido, EstadoPedido.REGISTRADO);
                cn.commit();
                pedido.asignarIdentidad(idPedido, codigo);
                return pedido;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar el pedido", e);
        }
    }

    private Integer registrarCliente(Connection cn, Cliente cliente) throws SQLException {
        if (!cliente.tieneDocumento()) {
            return null;
        }
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPSERT_CLIENTE)) {
            ps.setString(1, cliente.documento());
            ps.setString(2, cliente.nombre());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertarDetalle(Connection cn, long idPedido, ItemPedido item) throws SQLException {
        long idDetalle;
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_DETALLE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, idPedido);
            ps.setInt(2, item.getProducto().getId());
            ps.setInt(3, item.getCantidad());
            ps.setString(4, item.getTamano().name());
            ps.setBigDecimal(5, item.precioUnitario());
            ps.setBigDecimal(6, item.getDescuento());
            ps.setBigDecimal(7, item.subtotal());
            ps.setString(8, item.getEtiquetaCombo());
            ps.setString(9, item.getNota());
            ps.setString(10, recortar(item.descripcionComanda(), 400));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idDetalle = rs.getLong("id_detalle");
            }
        }
        if (!item.estaPersonalizado()) {
            return;
        }
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_PERSONALIZACION)) {
            for (Ingrediente ing : item.getIngredientesQuitados()) {
                agregarPersonalizacion(ps, idDetalle, ing, "QUITAR", BigDecimal.ZERO);
            }
            for (Ingrediente ing : item.getExtras()) {
                agregarPersonalizacion(ps, idDetalle, ing, "AGREGAR", ing.precioExtra());
            }
            ps.executeBatch();
        }
    }

    private static void agregarPersonalizacion(PreparedStatement ps, long idDetalle, Ingrediente ing,
                                               String accion, BigDecimal precio) throws SQLException {
        ps.setLong(1, idDetalle);
        ps.setInt(2, ing.id());
        ps.setString(3, accion);
        ps.setBigDecimal(4, precio);
        ps.addBatch();
    }

    private static void insertarHistorial(Connection cn, long idPedido, EstadoPedido estado) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_HISTORIAL)) {
            ps.setLong(1, idPedido);
            ps.setString(2, estado.name());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<EstadoPedido> obtenerEstado(long idPedido) {
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
            ps.setLong(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(EstadoPedido.valueOf(rs.getString(1))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo consultar el estado del pedido", e);
        }
    }

    @Override
    public void actualizarEstado(long idPedido, EstadoPedido nuevoEstado) {
        try (Connection cn = conexionBD.obtenerConexion()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_ESTADO)) {
                ps.setString(1, nuevoEstado.name());
                ps.setLong(2, idPedido);
                ps.executeUpdate();
                insertarHistorial(cn, idPedido, nuevoEstado);
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo actualizar el estado del pedido", e);
        }
    }

    @Override
    public List<ResumenPedido> listarPorEstados(Set<EstadoPedido> estados) {
        List<ResumenPedido> lista = new ArrayList<>();
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_COMANDAS)) {
            String[] nombres = estados.stream().map(Enum::name).toArray(String[]::new);
            ps.setArray(1, cn.createArrayOf("varchar", nombres));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ResumenPedido(rs.getLong("id_pedido"), rs.getString("codigo"),
                            EstadoPedido.valueOf(rs.getString("estado")),
                            CanalVenta.valueOf(rs.getString("canal")),
                            rs.getString("nombre_cliente"),
                            rs.getTimestamp("fecha_registro").toLocalDateTime(),
                            rs.getBigDecimal("total"), rs.getString("detalle")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudieron listar las comandas", e);
        }
    }

    @Override
    public ResumenVentas resumenDelDia(LocalDate fecha) {
        try (Connection cn = conexionBD.obtenerConexion()) {
            Date dia = Date.valueOf(fecha);
            int pedidos;
            BigDecimal vendido;
            try (PreparedStatement ps = cn.prepareStatement(SQL_RESUMEN)) {
                ps.setDate(1, dia);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    pedidos = rs.getInt("pedidos");
                    vendido = rs.getBigDecimal("vendido");
                }
            }
            Map<EstadoPedido, Integer> porEstado = new EnumMap<>(EstadoPedido.class);
            try (PreparedStatement ps = cn.prepareStatement(SQL_POR_ESTADO)) {
                ps.setDate(1, dia);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        porEstado.put(EstadoPedido.valueOf(rs.getString(1)), rs.getInt(2));
                    }
                }
            }
            int items;
            int personalizados;
            try (PreparedStatement ps = cn.prepareStatement(SQL_PERSONALIZADOS)) {
                ps.setDate(1, dia);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    items = rs.getInt("total");
                    personalizados = rs.getInt("personalizados");
                }
            }
            Double minutos;
            try (PreparedStatement ps = cn.prepareStatement(SQL_TIEMPO)) {
                ps.setDate(1, dia);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    BigDecimal valor = rs.getBigDecimal(1);
                    minutos = valor == null ? null : valor.doubleValue();
                }
            }
            return new ResumenVentas(fecha, pedidos, vendido, porEstado, personalizados, items, minutos);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo generar el resumen del día", e);
        }
    }

    @Override
    public int contar() {
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement("SELECT count(*) FROM pedido");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo contar los pedidos", e);
        }
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
