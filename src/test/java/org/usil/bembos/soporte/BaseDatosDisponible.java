package org.usil.bembos.soporte;

import org.usil.bembos.config.ConexionBD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Condicion para {@code @EnabledIf}: las pruebas de integracion con PostgreSQL
 * solo se ejecutan si el servidor responde y el esquema {@code bembos} tiene
 * el catalogo cargado. En caso contrario se marcan como omitidas (skipped).
 */
public final class BaseDatosDisponible {

    private static Boolean disponible;

    private BaseDatosDisponible() {
    }

    public static synchronized boolean hayBase() {
        if (disponible == null) {
            disponible = verificar();
        }
        return disponible;
    }

    private static boolean verificar() {
        try (Connection cn = ConexionBD.getInstancia().obtenerConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM bembos.producto")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("[INFO] PostgreSQL no disponible; se omiten pruebas de integración: " + e.getMessage());
            return false;
        }
    }
}
