package org.usil.bembos.model.entidad;

/**
 * Datos del cliente. El documento es opcional para boletas de bajo monto.
 */
public record Cliente(String documento, String nombre) {

    public static final Cliente CLIENTE_VARIOS = new Cliente("", "Clientes varios");

    public Cliente {
        documento = documento == null ? "" : documento.trim();
        nombre = (nombre == null || nombre.isBlank()) ? "Clientes varios" : nombre.trim();
    }

    public boolean tieneDocumento() {
        return !documento.isEmpty();
    }
}
