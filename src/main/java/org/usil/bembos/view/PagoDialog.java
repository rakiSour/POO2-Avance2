package org.usil.bembos.view;

import org.usil.bembos.controller.PagoController;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.MetodoPago;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.TipoComprobante;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;

/** Vista de cobro: metodo de pago, comprobante y vuelto. */
public class PagoDialog extends JDialog {

    private final PagoController controller;
    private final Pedido pedido;
    private final JComboBox<MetodoPago> cbMetodo = new JComboBox<>(MetodoPago.values());
    private final JTextField txtRecibido = new JTextField(10);
    private final JComboBox<TipoComprobante> cbComprobante = new JComboBox<>(TipoComprobante.values());
    private final JTextField txtDocumento = new JTextField(12);
    private final JTextField txtRazon = new JTextField(20);
    private final JLabel lblVuelto = new JLabel(" ");
    private Comprobante comprobante;

    public PagoDialog(Window padre, PagoController controller, Pedido pedido) {
        super(padre, "Cobrar pedido " + pedido.getCodigo(), ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.pedido = pedido;
        txtDocumento.setText(pedido.getCliente().documento());
        txtRazon.setText(pedido.getCliente().tieneDocumento() ? pedido.getCliente().nombre() : "");
        txtRecibido.setText(pedido.total().toPlainString());
        construir();
        pack();
        setLocationRelativeTo(padre);
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel total = new JLabel("Total a cobrar: " + Estilo.soles(pedido.total()));
        total.setFont(Estilo.TOTAL);
        total.setForeground(Estilo.AZUL);
        raiz.add(total, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Método de pago:"));
        form.add(cbMetodo);
        form.add(new JLabel("Monto recibido (S/):"));
        form.add(txtRecibido);
        form.add(new JLabel("Vuelto:"));
        form.add(lblVuelto);
        form.add(new JLabel("Comprobante:"));
        form.add(cbComprobante);
        form.add(new JLabel("DNI / RUC:"));
        form.add(txtDocumento);
        form.add(new JLabel("Nombre / Razón social:"));
        form.add(txtRazon);
        raiz.add(form, BorderLayout.CENTER);

        cbMetodo.addActionListener(e -> {
            boolean efectivo = cbMetodo.getSelectedItem() == MetodoPago.EFECTIVO;
            txtRecibido.setEnabled(efectivo);
            if (!efectivo) {
                txtRecibido.setText(pedido.total().toPlainString());
            }
            actualizarVuelto();
        });
        txtRecibido.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { actualizarVuelto(); }
            public void removeUpdate(DocumentEvent e) { actualizarVuelto(); }
            public void changedUpdate(DocumentEvent e) { actualizarVuelto(); }
        });
        actualizarVuelto();

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var btnCancelar = Estilo.boton("Cancelar");
        var btnCobrar = Estilo.botonPrimario("Cobrar y emitir");
        btnCancelar.addActionListener(e -> dispose());
        btnCobrar.addActionListener(e -> cobrar());
        botones.add(btnCancelar);
        botones.add(btnCobrar);
        raiz.add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnCobrar);
        setContentPane(raiz);
    }

    private BigDecimal montoRecibido() {
        try {
            return new BigDecimal(txtRecibido.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void actualizarVuelto() {
        BigDecimal recibido = montoRecibido();
        if (recibido == null) {
            lblVuelto.setText("monto inválido");
            lblVuelto.setForeground(Estilo.ROJO);
        } else if (recibido.compareTo(pedido.total()) < 0) {
            lblVuelto.setText("faltan " + Estilo.soles(pedido.total().subtract(recibido)));
            lblVuelto.setForeground(Estilo.ROJO);
        } else {
            lblVuelto.setText(Estilo.soles(controller.calcularVuelto(pedido.total(), recibido)));
            lblVuelto.setForeground(Estilo.VERDE);
        }
    }

    private void cobrar() {
        try {
            comprobante = controller.cobrar(pedido, (MetodoPago) cbMetodo.getSelectedItem(), montoRecibido(),
                    (TipoComprobante) cbComprobante.getSelectedItem(), txtDocumento.getText(), txtRazon.getText());
            dispose();
        } catch (ReglaNegocioException ex) {
            Estilo.mostrarErrores(this, "No se pudo cobrar", ex.getErrores());
        } catch (AccesoDatosException ex) {
            Estilo.mostrarError(this, ex.getMessage());
        }
    }

    /** Comprobante emitido o null si se cancelo el cobro. */
    public Comprobante getComprobante() {
        return comprobante;
    }
}
