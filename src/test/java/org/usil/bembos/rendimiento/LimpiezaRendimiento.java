package org.usil.bembos.rendimiento;

import org.usil.bembos.config.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;

/** Borra de PostgreSQL los pedidos generados por las pruebas de estres. */
final class LimpiezaRendimiento {

    private LimpiezaRendimiento() {
    }

    static void eliminar(Collection<Long> ids) throws Exception {
        Long[] arreglo = ids.toArray(new Long[0]);
        try (Connection cn = ConexionBD.getInstancia().obtenerConexion()) {
            cn.setAutoCommit(false);
            for (String tabla : new String[]{"comprobante", "pago", "pedido"}) {
                try (PreparedStatement ps = cn.prepareStatement("DELETE FROM " + tabla + " WHERE id_pedido = ANY (?)")) {
                    ps.setArray(1, cn.createArrayOf("bigint", arreglo));
                    ps.executeUpdate();
                }
            }
            cn.commit();
        }
    }
}
