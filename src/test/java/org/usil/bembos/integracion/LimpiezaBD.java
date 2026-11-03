package org.usil.bembos.integracion;

import org.usil.bembos.config.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;

/** Elimina los pedidos creados por las pruebas para no ensuciar la base de datos. */
final class LimpiezaBD {

    private LimpiezaBD() {
    }

    static void eliminarPedidos(Collection<Long> ids) throws Exception {
        if (ids.isEmpty()) {
            return;
        }
        Long[] arreglo = ids.toArray(new Long[0]);
        try (Connection cn = ConexionBD.getInstancia().obtenerConexion()) {
            cn.setAutoCommit(false);
            for (String tabla : new String[]{"comprobante", "pago", "pedido"}) {
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM " + tabla + " WHERE id_pedido = ANY (?)")) {
                    ps.setArray(1, cn.createArrayOf("bigint", arreglo));
                    ps.executeUpdate();
                }
            }
            cn.commit();
        }
    }
}
