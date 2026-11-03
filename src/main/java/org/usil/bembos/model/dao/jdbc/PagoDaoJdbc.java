package org.usil.bembos.model.dao.jdbc;

import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.dao.PagoDAO;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Pago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PagoDaoJdbc implements PagoDAO {

    private static final String SQL_PAGO = """
            INSERT INTO pago (id_pedido, metodo, monto, monto_recibido, vuelto, referencia, fecha)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    private static final String SQL_COMPROBANTE = """
            INSERT INTO comprobante (id_pedido, tipo, serie, numero, documento_cliente, razon_social,
                                     total, fecha_emision)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

    private final ConexionBD conexionBD;

    public PagoDaoJdbc(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    @Override
    public void registrar(Pago pago, Comprobante comprobante) {
        try (Connection cn = conexionBD.obtenerConexion()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps = cn.prepareStatement(SQL_PAGO);
                 PreparedStatement pc = cn.prepareStatement(SQL_COMPROBANTE)) {
                ps.setLong(1, pago.idPedido());
                ps.setString(2, pago.metodo().name());
                ps.setBigDecimal(3, pago.monto());
                ps.setBigDecimal(4, pago.montoRecibido());
                ps.setBigDecimal(5, pago.vuelto());
                ps.setString(6, pago.referencia());
                ps.setTimestamp(7, Timestamp.valueOf(pago.fecha()));
                ps.executeUpdate();

                pc.setLong(1, pago.idPedido());
                pc.setString(2, comprobante.tipo().name());
                pc.setString(3, comprobante.serie());
                pc.setInt(4, comprobante.numero());
                pc.setString(5, comprobante.documentoCliente());
                pc.setString(6, comprobante.razonSocial());
                pc.setBigDecimal(7, comprobante.total());
                pc.setTimestamp(8, Timestamp.valueOf(comprobante.fechaEmision()));
                pc.executeUpdate();
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar el pago", e);
        }
    }

    @Override
    public int ultimoCorrelativo(String serie) {
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT coalesce(max(numero), 0) FROM comprobante WHERE serie = ?")) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo leer el correlativo de " + serie, e);
        }
    }
}
