package org.usil.bembos.model.servicio.facturacion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase adaptada (Adaptee) del patron Adapter.
 * Simula el modulo de facturacion heredado de la franquicia: trabaja con
 * tramas de texto separadas por "|" y responde cadenas "OK;serie;numero" o
 * "ERR;mensaje". Su interfaz es incompatible con el modelo orientado a
 * objetos del nuevo sistema y no debe modificarse.
 */
public class SistemaFacturacionLegacy {

    private final Map<String, AtomicInteger> correlativos = new ConcurrentHashMap<>();

    /** Continua la numeracion desde el ultimo comprobante emitido (p. ej., leido de la BD). */
    public void sincronizarCorrelativo(String serie, int ultimoNumero) {
        correlativos.computeIfAbsent(serie, s -> new AtomicInteger())
                .accumulateAndGet(ultimoNumero, Math::max);
    }

    /**
     * @param trama formato {@code TIPO|DOCUMENTO|RAZON_SOCIAL|TOTAL}
     *              donde TIPO es 01 (factura) o 03 (boleta)
     */
    public String procesarTrama(String trama) {
        String[] campos = trama.split("\\|", -1);
        if (campos.length != 4) {
            return "ERR;Trama con formato inválido";
        }
        String tipo = campos[0];
        String documento = campos[1];
        String razonSocial = campos[2];
        double total;
        try {
            total = Double.parseDouble(campos[3]);
        } catch (NumberFormatException e) {
            return "ERR;Importe inválido";
        }
        String serie;
        switch (tipo) {
            case "01" -> {
                if (!documento.matches("\\d{11}")) {
                    return "ERR;La factura requiere un RUC de 11 dígitos";
                }
                if (razonSocial.isBlank()) {
                    return "ERR;La factura requiere razón social";
                }
                serie = "F001";
            }
            case "03" -> {
                if (!documento.isEmpty() && !documento.matches("\\d{8}|\\d{11}")) {
                    return "ERR;El documento de la boleta debe ser DNI (8) o RUC (11)";
                }
                if (total > 700.0 && documento.isEmpty()) {
                    return "ERR;Boletas mayores a S/ 700 requieren identificar al cliente";
                }
                serie = "B001";
            }
            default -> {
                return "ERR;Tipo de comprobante no soportado: " + tipo;
            }
        }
        int numero = correlativos.computeIfAbsent(serie, s -> new AtomicInteger()).incrementAndGet();
        return "OK;" + serie + ";" + numero;
    }
}
