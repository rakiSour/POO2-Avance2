package org.usil.bembos.model.servicio.facturacion;

import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * <b>Patron Adapter (estructural).</b>
 * Traduce la interfaz orientada a objetos {@link ServicioFacturacion} a las
 * tramas de texto que entiende {@link SistemaFacturacionLegacy}, y convierte
 * la respuesta de texto en un objeto {@link Comprobante}.
 */
public class FacturacionLegacyAdapter implements ServicioFacturacion {

    private final SistemaFacturacionLegacy legacy;

    public FacturacionLegacyAdapter(SistemaFacturacionLegacy legacy) {
        this.legacy = legacy;
    }

    @Override
    public Comprobante emitir(Pedido pedido, TipoComprobante tipo, String documentoCliente, String razonSocial) {
        String documento = documentoCliente == null ? "" : documentoCliente.trim();
        String razon = razonSocial == null || razonSocial.isBlank()
                ? pedido.getCliente().nombre() : razonSocial.trim();
        String codigoTipo = tipo == TipoComprobante.FACTURA ? "01" : "03";
        String trama = String.join("|", codigoTipo, documento, razon.replace("|", " "),
                String.format(Locale.ROOT, "%.2f", pedido.total()));

        String respuesta = legacy.procesarTrama(trama);
        String[] partes = respuesta.split(";");
        if (!"OK".equals(partes[0])) {
            throw new ReglaNegocioException(partes.length > 1 ? partes[1] : "Error de facturación");
        }
        return new Comprobante(tipo, partes[1], Integer.parseInt(partes[2]), documento, razon,
                pedido.total(), LocalDateTime.now());
    }
}
