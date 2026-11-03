package org.usil.bembos.view;

import org.usil.bembos.controller.CocinaController;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.ResumenPedido;
import org.usil.bembos.model.excepcion.ReglaNegocioException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <b>Vista (MVC)</b> de cocina (KDS). Se refresca cada 5 segundos y permite
 * avanzar el pedido: Pagado -> En preparacion -> Listo -> Entregado.
 */
public class CocinaPanel extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CocinaController controller;
    private final ComandasModel modelo = new ComandasModel();
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel();
    private final Timer timer;

    public CocinaPanel(CocinaController controller) {
        super(new BorderLayout(8, 8));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel norte = new JPanel(new BorderLayout());
        JLabel titulo = new JLabel("Pantalla de cocina (KDS)");
        titulo.setFont(Estilo.TITULO);
        titulo.setForeground(Estilo.AZUL);
        norte.add(titulo, BorderLayout.WEST);
        norte.add(lblResumen, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        tabla.setRowHeight(64);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {90, 70, 140, 60, 150, 560};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(5).setCellRenderer(new DetalleRenderer());
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                if (!sel) {
                    comp.setBackground(colorEstado(modelo.get(f).estado()));
                }
                return comp;
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAvanzar = Estilo.botonPrimario("Avanzar estado ▶");
        JButton btnAnular = Estilo.boton("Anular");
        JButton btnRefrescar = Estilo.boton("Refrescar");
        btnAvanzar.addActionListener(e -> avanzar());
        btnAnular.addActionListener(e -> anular());
        btnRefrescar.addActionListener(e -> refrescar());
        botones.add(new JLabel("Amarillo: en cola · Azul: en preparación · Verde: listo   "));
        botones.add(btnRefrescar);
        botones.add(btnAnular);
        botones.add(btnAvanzar);
        add(botones, BorderLayout.SOUTH);

        timer = new Timer(5000, e -> refrescar());
        timer.start();
        refrescar();
    }

    private static Color colorEstado(EstadoPedido e) {
        return switch (e) {
            case PAGADO -> Estilo.AMARILLO_SUAVE;
            case EN_PREPARACION -> Estilo.AZUL_CLARO;
            case LISTO -> Estilo.VERDE_SUAVE;
            default -> Color.WHITE;
        };
    }

    public void refrescar() {
        try {
            long idSeleccionado = tabla.getSelectedRow() >= 0 ? modelo.get(tabla.getSelectedRow()).id() : -1;
            List<ResumenPedido> comandas = controller.comandasPendientes();
            modelo.setDatos(comandas);
            for (int i = 0; i < comandas.size(); i++) {
                int lineas = Math.max(2, comandas.get(i).detalle().split("\n").length);
                tabla.setRowHeight(i, lineas * 22 + 12);
            }
            for (int i = 0; i < comandas.size(); i++) {
                if (comandas.get(i).id() == idSeleccionado) {
                    tabla.setRowSelectionInterval(i, i);
                }
            }
            long enCola = comandas.stream().filter(c -> c.estado() == EstadoPedido.PAGADO).count();
            long enPrep = comandas.stream().filter(c -> c.estado() == EstadoPedido.EN_PREPARACION).count();
            long listos = comandas.stream().filter(c -> c.estado() == EstadoPedido.LISTO).count();
            lblResumen.setText("En cola: " + enCola + "   En preparación: " + enPrep + "   Listos: " + listos + "  ");
        } catch (AccesoDatosException ex) {
            lblResumen.setText("Sin conexión con la base de datos");
        }
    }

    private ResumenPedido seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una comanda.");
            return null;
        }
        return modelo.get(fila);
    }

    private void avanzar() {
        ResumenPedido r = seleccionado();
        if (r == null) return;
        try {
            controller.avanzar(r.id());
            refrescar();
        } catch (ReglaNegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void anular() {
        ResumenPedido r = seleccionado();
        if (r == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Anular el pedido " + r.codigo() + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.anular(r.id());
            refrescar();
        } catch (ReglaNegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private static final class DetalleRenderer extends JTextArea implements TableCellRenderer {
        DetalleRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(Estilo.NORMAL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            setText(v == null ? "" : v.toString());
            ComandasModel m = (ComandasModel) t.getModel();
            setBackground(sel ? t.getSelectionBackground() : colorEstado(m.get(f).estado()));
            setForeground(sel ? t.getSelectionForeground() : Color.BLACK);
            return this;
        }
    }

    private static final class ComandasModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Pedido", "Hora", "Estado", "Min.", "Cliente / canal", "Detalle"};
        private List<ResumenPedido> datos = List.of();

        void setDatos(List<ResumenPedido> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        ResumenPedido get(int fila) {
            return datos.get(fila);
        }

        @Override
        public int getRowCount() {
            return datos.size();
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
            ResumenPedido r = datos.get(f);
            return switch (c) {
                case 0 -> r.codigo();
                case 1 -> HORA.format(r.fechaRegistro());
                case 2 -> r.estado().getEtiqueta();
                case 3 -> r.minutosTranscurridos();
                case 4 -> r.cliente() + " / " + r.canal();
                default -> r.detalle();
            };
        }
    }
}
