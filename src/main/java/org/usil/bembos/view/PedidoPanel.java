package org.usil.bembos.view;

import org.usil.bembos.controller.PagoController;
import org.usil.bembos.controller.PedidoController;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.entidad.CanalVenta;
import org.usil.bembos.model.entidad.Comprobante;
import org.usil.bembos.model.entidad.ItemPedido;
import org.usil.bembos.model.entidad.Pedido;
import org.usil.bembos.model.entidad.Producto;
import org.usil.bembos.model.entidad.Tamano;
import org.usil.bembos.model.entidad.TipoProducto;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * <b>Vista (MVC)</b> de toma de pedidos: catalogo, combos, carrito y
 * datos del cliente. Toda accion se delega a {@link PedidoController}.
 */
public class PedidoPanel extends JPanel {

    private final PedidoController controller;
    private final PagoController pagoController;
    private final Runnable alRegistrarPedido;

    private final JComboBox<Object> cbFamilia = new JComboBox<>();
    private final DefaultListModel<Producto> modeloCatalogo = new DefaultListModel<>();
    private final JList<Producto> lstCatalogo = new JList<>(modeloCatalogo);
    private final JComboBox<String> cbCombo;
    private final JComboBox<Tamano> cbTamanoCombo = new JComboBox<>(Tamano.values());
    private final CarritoModel modeloCarrito = new CarritoModel();
    private final JTable tblCarrito = new JTable(modeloCarrito);
    private final JTextField txtDocumento = new JTextField(10);
    private final JTextField txtNombre = new JTextField(16);
    private final JComboBox<CanalVenta> cbCanal = new JComboBox<>(CanalVenta.values());
    private final JTextField txtObservacion = new JTextField(20);
    private final JLabel lblTotal = new JLabel();

    public PedidoPanel(PedidoController controller, PagoController pagoController, Runnable alRegistrarPedido) {
        super(new BorderLayout(8, 8));
        this.controller = controller;
        this.pagoController = pagoController;
        this.alRegistrarPedido = alRegistrarPedido;
        this.cbCombo = new JComboBox<>(controller.getCombos().toArray(new String[0]));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Estilo.FONDO);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCatalogo(), panelPedido());
        split.setResizeWeight(0.36);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        cargarCatalogo();
        refrescarCarrito();
    }

    // ----------------------------------------------------------------- catalogo

    private JPanel panelCatalogo() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(Estilo.marco("1. Catálogo"));

        cbFamilia.addItem("Todas las familias");
        for (TipoProducto t : TipoProducto.values()) {
            cbFamilia.addItem(t);
        }
        cbFamilia.addActionListener(e -> cargarCatalogo());
        p.add(cbFamilia, BorderLayout.NORTH);

        lstCatalogo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCatalogo.setFixedCellHeight(28);
        lstCatalogo.setCellRenderer((list, prod, idx, sel, foco) -> {
            JLabel l = new JLabel("  " + prod.getNombre() + "   " + Estilo.soles(prod.getPrecioBase())
                    + (prod.esPersonalizable() ? "   ✎" : ""));
            l.setOpaque(true);
            l.setBackground(sel ? Estilo.AZUL : (idx % 2 == 0 ? java.awt.Color.WHITE : Estilo.AZUL_CLARO));
            l.setForeground(sel ? java.awt.Color.WHITE : java.awt.Color.DARK_GRAY);
            return l;
        });
        lstCatalogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    personalizarSeleccionado();
                }
            }
        });
        p.add(new JScrollPane(lstCatalogo), BorderLayout.CENTER);

        JPanel sur = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;
        JButton btnPersonalizar = Estilo.botonPrimario("Personalizar y agregar");
        btnPersonalizar.addActionListener(e -> personalizarSeleccionado());
        g.gridx = 0; g.gridy = 0; g.gridwidth = 3;
        sur.add(btnPersonalizar, g);

        g.gridy = 1; g.gridwidth = 3;
        sur.add(Estilo.subtitulo("Combos (Director del Builder)"), g);
        g.gridy = 2; g.gridwidth = 1; g.weightx = 1;
        sur.add(cbCombo, g);
        g.gridx = 1; g.weightx = 0;
        sur.add(cbTamanoCombo, g);
        JButton btnCombo = Estilo.boton("Agregar combo");
        btnCombo.addActionListener(e -> agregarCombo());
        g.gridx = 2;
        sur.add(btnCombo, g);
        p.add(sur, BorderLayout.SOUTH);
        return p;
    }

    private void cargarCatalogo() {
        Object sel = cbFamilia.getSelectedItem();
        modeloCatalogo.clear();
        controller.productosPorTipo(sel instanceof TipoProducto t ? t : null).forEach(modeloCatalogo::addElement);
    }

    private void personalizarSeleccionado() {
        Producto p = lstCatalogo.getSelectedValue();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto del catálogo.");
            return;
        }
        PersonalizacionDialog dialogo = new PersonalizacionDialog(SwingUtilities.getWindowAncestor(this), controller, p);
        dialogo.setVisible(true);
        if (dialogo.fueAgregado()) {
            refrescarCarrito();
        }
    }

    private void agregarCombo() {
        try {
            controller.agregarCombo((String) cbCombo.getSelectedItem(), (Tamano) cbTamanoCombo.getSelectedItem());
            refrescarCarrito();
        } catch (ReglaNegocioException ex) {
            Estilo.mostrarErrores(this, "Combo no disponible", ex.getErrores());
        }
    }

    // ----------------------------------------------------------------- pedido

    private JPanel panelPedido() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(Estilo.marco("2. Pedido en curso"));

        tblCarrito.setRowHeight(26);
        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(420);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(90);
        tblCarrito.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                setToolTipText(c == 1 && v != null ? v.toString() : null);
                setHorizontalAlignment(c >= 2 ? RIGHT : (c == 0 ? CENTER : LEFT));
                return comp;
            }
        });
        p.add(new JScrollPane(tblCarrito), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnQuitar = Estilo.boton("Quitar ítem");
        JButton btnVaciar = Estilo.boton("Vaciar pedido");
        btnQuitar.addActionListener(e -> quitarItem());
        btnVaciar.addActionListener(e -> {
            controller.cancelarPedidoEnCurso();
            refrescarCarrito();
        });
        acciones.add(btnQuitar);
        acciones.add(btnVaciar);

        JPanel cliente = new JPanel(new GridBagLayout());
        cliente.setBorder(Estilo.marco("3. Cliente y confirmación"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        cliente.add(new JLabel("DNI/RUC:"), g);
        g.gridx = 1;
        cliente.add(txtDocumento, g);
        g.gridx = 2;
        cliente.add(new JLabel("Nombre:"), g);
        g.gridx = 3;
        cliente.add(txtNombre, g);
        g.gridx = 0; g.gridy = 1;
        cliente.add(new JLabel("Canal:"), g);
        g.gridx = 1;
        cliente.add(cbCanal, g);
        g.gridx = 2;
        cliente.add(new JLabel("Observación:"), g);
        g.gridx = 3;
        cliente.add(txtObservacion, g);

        lblTotal.setFont(Estilo.TOTAL);
        lblTotal.setForeground(Estilo.AZUL);
        JButton btnConfirmar = Estilo.botonPrimario("Confirmar y cobrar");
        btnConfirmar.addActionListener(e -> confirmar());
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        totalPanel.add(lblTotal);
        totalPanel.add(btnConfirmar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(acciones, BorderLayout.NORTH);
        sur.add(cliente, BorderLayout.CENTER);
        sur.add(totalPanel, BorderLayout.SOUTH);
        p.add(sur, BorderLayout.SOUTH);
        return p;
    }

    private void quitarItem() {
        int fila = tblCarrito.getSelectedRow();
        try {
            controller.quitarItem(fila);
            refrescarCarrito();
        } catch (ReglaNegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void refrescarCarrito() {
        modeloCarrito.setItems(controller.getItems());
        lblTotal.setText("Total: " + Estilo.soles(controller.getTotal()));
    }

    private void confirmar() {
        Pedido pedido;
        try {
            pedido = controller.confirmarPedido(txtDocumento.getText(), txtNombre.getText(),
                    (CanalVenta) cbCanal.getSelectedItem(), txtObservacion.getText());
        } catch (ReglaNegocioException ex) {
            Estilo.mostrarErrores(this, "Pedido incompleto", ex.getErrores());
            return;
        } catch (AccesoDatosException ex) {
            Estilo.mostrarError(this, ex.getMessage());
            return;
        }
        refrescarCarrito();
        PagoDialog pago = new PagoDialog(SwingUtilities.getWindowAncestor(this), pagoController, pedido);
        pago.setVisible(true);
        Comprobante comprobante = pago.getComprobante();
        if (comprobante != null) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Pedido " + pedido.getCodigo() + " enviado a cocina.</b><br>"
                            + comprobante + "<br>Total: " + Estilo.soles(pedido.total()) + "</html>",
                    "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
            limpiarCliente();
        } else {
            int r = JOptionPane.showConfirmDialog(this,
                    "El pedido " + pedido.getCodigo() + " quedó registrado sin pago.\n¿Desea anularlo?",
                    "Pago cancelado", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                controller.getFacade().anularPedido(pedido.getId());
            }
        }
        alRegistrarPedido.run();
    }

    private void limpiarCliente() {
        txtDocumento.setText("");
        txtNombre.setText("");
        txtObservacion.setText("");
        cbCanal.setSelectedItem(CanalVenta.MOSTRADOR);
    }

    /** Modelo de tabla del carrito. */
    private static final class CarritoModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Cant.", "Descripción", "P. unit.", "Desc.", "Subtotal"};
        private List<ItemPedido> items = List.of();

        void setItems(List<ItemPedido> items) {
            this.items = List.copyOf(items);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int c) {
            return COLUMNAS[c];
        }

        @Override
        public Object getValueAt(int f, int c) {
            ItemPedido i = items.get(f);
            return switch (c) {
                case 0 -> i.getCantidad();
                case 1 -> i.descripcionComanda().replaceFirst("^\\d+ x ", "");
                case 2 -> Estilo.soles(i.precioUnitario());
                case 3 -> i.getDescuento().signum() == 0 ? "" : "-" + Estilo.soles(i.getDescuento());
                default -> Estilo.soles(i.subtotal());
            };
        }
    }
}
