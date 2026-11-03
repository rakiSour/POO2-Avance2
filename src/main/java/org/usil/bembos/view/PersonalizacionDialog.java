package org.usil.bembos.view;

import org.usil.bembos.controller.PedidoController;
import org.usil.bembos.model.entidad.Hamburguesa;
import org.usil.bembos.model.entidad.Ingrediente;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista para personalizar un producto antes de agregarlo al pedido.
 * Solo recoge la intencion del usuario; las reglas las aplica el Builder.
 */
public class PersonalizacionDialog extends JDialog {

    private final PedidoController controller;
    private final Producto producto;
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JComboBox<Tamano> cbTamano;
    private final Map<Ingrediente, JCheckBox> receta = new LinkedHashMap<>();
    private final Map<Ingrediente, JSpinner> extras = new LinkedHashMap<>();
    private final JTextField txtNota = new JTextField(24);
    private final JLabel lblPrecio = new JLabel();
    private boolean agregado;

    public PersonalizacionDialog(Window padre, PedidoController controller, Producto producto) {
        super(padre, "Personalizar: " + producto.getNombre(), ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.producto = producto;
        this.cbTamano = new JComboBox<>(producto.tamanosPermitidos().toArray(new Tamano[0]));
        this.cbTamano.setPrototypeDisplayValue(Tamano.AGRANDADO);
        this.cbTamano.setPreferredSize(new java.awt.Dimension(130, 28));
        construir();
        actualizarPrecio();
        pack();
        setLocationRelativeTo(padre);
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel titulo = new JLabel(producto.getNombre() + "  —  " + Estilo.soles(producto.getPrecioBase()));
        titulo.setFont(Estilo.TITULO);
        titulo.setForeground(Estilo.AZUL);
        raiz.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        JPanel basicos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        basicos.add(new JLabel("Cantidad:"));
        basicos.add(spCantidad);
        basicos.add(new JLabel("   Tamaño:"));
        basicos.add(cbTamano);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        centro.add(basicos, g);

        if (producto instanceof Hamburguesa h) {
            JPanel pReceta = new JPanel();
            pReceta.setLayout(new BoxLayout(pReceta, BoxLayout.Y_AXIS));
            pReceta.setBorder(Estilo.marco("Receta (desmarque para quitar)"));
            for (Map.Entry<Ingrediente, Boolean> e : h.getReceta().entrySet()) {
                boolean obligatorio = e.getValue();
                JCheckBox chk = new JCheckBox(e.getKey().nombre() + (obligatorio ? "  (obligatorio)" : ""), true);
                chk.setEnabled(!obligatorio);
                receta.put(e.getKey(), chk);
                pReceta.add(chk);
            }
            JPanel pExtras = new JPanel(new GridLayout(0, 2, 10, 4));
            pExtras.setBorder(Estilo.marco("Adicionales"));
            for (Ingrediente ing : controller.getAdicionales()) {
                JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
                ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(2);
                sp.setEnabled(ing.disponible());
                sp.addChangeListener(ev -> actualizarPrecio());
                JLabel lbl = new JLabel(ing.nombre() + " (+" + Estilo.soles(ing.precioExtra()) + ")"
                        + (ing.disponible() ? "" : " – agotado"));
                if (!ing.disponible()) {
                    lbl.setForeground(Estilo.ROJO);
                }
                pExtras.add(lbl);
                pExtras.add(sp);
                extras.put(ing, sp);
            }
            g.gridy = 1; g.gridwidth = 1; g.gridx = 0; g.anchor = GridBagConstraints.NORTHWEST;
            g.fill = GridBagConstraints.BOTH;
            centro.add(pReceta, g);
            g.gridx = 1;
            JScrollPane scroll = new JScrollPane(pExtras,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setPreferredSize(new java.awt.Dimension(pExtras.getPreferredSize().width + 24, 320));
            centro.add(scroll, g);
        }

        JPanel pNota = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pNota.add(new JLabel("Nota para cocina:"));
        pNota.add(txtNota);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        centro.add(pNota, g);
        raiz.add(centro, BorderLayout.CENTER);

        spCantidad.addChangeListener(e -> actualizarPrecio());
        cbTamano.addActionListener(e -> actualizarPrecio());

        JPanel sur = new JPanel(new BorderLayout());
        lblPrecio.setFont(Estilo.SUBTITULO);
        lblPrecio.setForeground(Estilo.AZUL);
        sur.add(lblPrecio, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var btnCancelar = Estilo.boton("Cancelar");
        var btnAgregar = Estilo.botonPrimario("Agregar al pedido");
        btnCancelar.addActionListener(e -> dispose());
        btnAgregar.addActionListener(e -> agregar());
        botones.add(btnCancelar);
        botones.add(btnAgregar);
        sur.add(botones, BorderLayout.EAST);
        raiz.add(sur, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnAgregar);
        setContentPane(raiz);
    }

    private Map<Ingrediente, Integer> extrasSeleccionados() {
        Map<Ingrediente, Integer> seleccion = new LinkedHashMap<>();
        extras.forEach((ing, sp) -> {
            int n = (Integer) sp.getValue();
            if (n > 0) {
                seleccion.put(ing, n);
            }
        });
        return seleccion;
    }

    private void actualizarPrecio() {
        Tamano t = (Tamano) cbTamano.getSelectedItem();
        var unitario = controller.previsualizarPrecio(producto, t == null ? Tamano.REGULAR : t, extrasSeleccionados());
        int cantidad = (Integer) spCantidad.getValue();
        lblPrecio.setText("Precio unitario: " + Estilo.soles(unitario) + "   |   Importe: "
                + Estilo.soles(unitario.multiply(java.math.BigDecimal.valueOf(cantidad))));
    }

    private void agregar() {
        List<Ingrediente> quitar = new ArrayList<>();
        receta.forEach((ing, chk) -> {
            if (!chk.isSelected()) {
                quitar.add(ing);
            }
        });
        try {
            controller.agregarProducto(producto, (Integer) spCantidad.getValue(),
                    (Tamano) cbTamano.getSelectedItem(), quitar, extrasSeleccionados(), txtNota.getText());
            agregado = true;
            dispose();
        } catch (ReglaNegocioException ex) {
            Estilo.mostrarErrores(this, "Personalización no válida", ex.getErrores());
        }
    }

    public boolean fueAgregado() {
        return agregado;
    }
}
