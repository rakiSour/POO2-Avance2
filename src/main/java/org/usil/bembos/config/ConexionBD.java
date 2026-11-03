package org.usil.bembos.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <b>Patron Singleton (creacional).</b>
 * Unico punto de acceso a PostgreSQL. Mantiene un pool de conexiones
 * reutilizables para no saturar el servidor cuando varias cajas, kioscos o
 * la pantalla de cocina consultan la base de datos al mismo tiempo.
 * <p>
 * La configuracion se lee de {@code db.properties} y puede sobrescribirse con
 * propiedades del sistema ({@code -Dbembos.db.url=...}) o variables de entorno
 * ({@code BEMBOS_DB_URL}, {@code BEMBOS_DB_USER}, {@code BEMBOS_DB_PASSWORD}).
 * <p>
 * Las conexiones entregadas son proxies: al llamar a {@code close()} la
 * conexion fisica no se cierra, sino que vuelve al pool.
 */
public final class ConexionBD {

    private static volatile ConexionBD instancia;

    private final String url;
    private final String usuario;
    private final String clave;
    private final int tamanoMaximo;
    private final long esperaMaximaMs;

    private final BlockingQueue<Connection> disponibles = new LinkedBlockingQueue<>();
    private final List<Connection> todas = new ArrayList<>();
    private final AtomicInteger creadas = new AtomicInteger();

    private ConexionBD(Properties cfg) {
        this.url = valor(cfg, "bembos.db.url", "BEMBOS_DB_URL",
                "jdbc:postgresql://localhost:5432/bembos_db?currentSchema=bembos");
        this.usuario = valor(cfg, "bembos.db.user", "BEMBOS_DB_USER", "postgres");
        this.clave = valor(cfg, "bembos.db.password", "BEMBOS_DB_PASSWORD", "postgres");
        this.tamanoMaximo = Integer.parseInt(valor(cfg, "bembos.db.pool.max", "BEMBOS_DB_POOL_MAX", "10"));
        this.esperaMaximaMs = Long.parseLong(valor(cfg, "bembos.db.pool.timeoutMs", "BEMBOS_DB_POOL_TIMEOUT", "10000"));
    }

    /** Devuelve la unica instancia (inicializacion perezosa y segura entre hilos). */
    public static ConexionBD getInstancia() {
        ConexionBD local = instancia;
        if (local == null) {
            synchronized (ConexionBD.class) {
                local = instancia;
                if (local == null) {
                    local = new ConexionBD(cargarPropiedades());
                    instancia = local;
                }
            }
        }
        return local;
    }

    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = ConexionBD.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer db.properties: " + e.getMessage());
        }
        return p;
    }

    private static String valor(Properties cfg, String clave, String variableEntorno, String porDefecto) {
        String sistema = System.getProperty(clave);
        if (sistema != null && !sistema.isBlank()) return sistema;
        String entorno = System.getenv(variableEntorno);
        if (entorno != null && !entorno.isBlank()) return entorno;
        return cfg.getProperty(clave, porDefecto);
    }

    /**
     * Obtiene una conexion del pool. Debe usarse con try-with-resources para
     * que regrese al pool automaticamente.
     */
    public Connection obtenerConexion() throws SQLException {
        Connection fisica = disponibles.poll();
        while (fisica != null && !esValida(fisica)) {
            descartar(fisica);
            fisica = disponibles.poll();
        }
        if (fisica == null) {
            fisica = crearSiHayCupo();
        }
        if (fisica == null) {
            try {
                fisica = disponibles.poll(esperaMaximaMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Espera de conexión interrumpida", e);
            }
            if (fisica == null) {
                throw new SQLException("No hay conexiones disponibles en el pool (máximo " + tamanoMaximo + ")");
            }
        }
        return envolver(fisica);
    }

    private Connection crearSiHayCupo() throws SQLException {
        while (true) {
            int actual = creadas.get();
            if (actual >= tamanoMaximo) {
                return null;
            }
            if (creadas.compareAndSet(actual, actual + 1)) {
                try {
                    Connection nueva = DriverManager.getConnection(url, usuario, clave);
                    synchronized (todas) {
                        todas.add(nueva);
                    }
                    return nueva;
                } catch (SQLException e) {
                    creadas.decrementAndGet();
                    throw e;
                }
            }
        }
    }

    private boolean esValida(Connection c) {
        try {
            return !c.isClosed() && c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void descartar(Connection c) {
        try {
            c.close();
        } catch (SQLException ignorada) {
            // la conexion ya estaba rota
        }
        synchronized (todas) {
            todas.remove(c);
        }
        creadas.decrementAndGet();
    }

    private void devolver(Connection fisica) {
        try {
            if (!fisica.getAutoCommit()) {
                fisica.rollback();
                fisica.setAutoCommit(true);
            }
            disponibles.offer(fisica);
        } catch (SQLException e) {
            descartar(fisica);
        }
    }

    private Connection envolver(Connection fisica) {
        AtomicBoolean cerrada = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(
                ConexionBD.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    switch (metodo.getName()) {
                        case "close" -> {
                            if (cerrada.compareAndSet(false, true)) {
                                devolver(fisica);
                            }
                            return null;
                        }
                        case "isClosed" -> {
                            return cerrada.get() || fisica.isClosed();
                        }
                        default -> {
                            if (cerrada.get()) {
                                throw new SQLException("La conexión ya fue devuelta al pool");
                            }
                            try {
                                return metodo.invoke(fisica, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    }
                });
    }

    /** Verifica que el servidor responda (usado al iniciar la aplicacion). */
    public boolean probarConexion() {
        try (Connection c = obtenerConexion()) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    public int conexionesCreadas() {
        return creadas.get();
    }

    public int conexionesLibres() {
        return disponibles.size();
    }

    public int getTamanoMaximo() {
        return tamanoMaximo;
    }

    public String getUrl() {
        return url;
    }

    /** Cierra todas las conexiones fisicas (al salir de la aplicacion). */
    public void cerrarTodo() {
        synchronized (todas) {
            for (Connection c : todas) {
                try {
                    c.close();
                } catch (SQLException ignorada) {
                    // se esta cerrando la aplicacion
                }
            }
            todas.clear();
        }
        disponibles.clear();
        creadas.set(0);
    }
}
