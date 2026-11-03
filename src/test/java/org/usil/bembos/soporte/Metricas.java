package org.usil.bembos.soporte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Registra las metricas obtenidas en las pruebas de rendimiento y simulacion
 * en {@code target/metricas/<archivo>.txt}, para documentar los resultados.
 */
public final class Metricas {

    private static final Path CARPETA = Path.of("target", "metricas");

    private Metricas() {
    }

    public static synchronized void registrar(String archivo, String clave, Object valor) {
        String linea = clave + "=" + valor;
        System.out.println("[METRICA] " + archivo + " :: " + linea);
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(CARPETA.resolve(archivo + ".txt"), linea + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir la métrica: " + e.getMessage());
        }
    }

    public static synchronized void iniciar(String archivo) {
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(CARPETA.resolve(archivo + ".txt"), "# " + LocalDateTime.now() + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el archivo de métricas: " + e.getMessage());
        }
    }
}
