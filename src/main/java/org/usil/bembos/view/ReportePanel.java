package org.usil.bembos.view;

import org.usil.bembos.controller.ReporteController;
import org.usil.bembos.model.dao.AccesoDatosException;
import org.usil.bembos.model.entidad.EstadoPedido;
import org.usil.bembos.model.entidad.ResumenVentas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** <b>Vista (MVC)</b> de indicadores del dia. */
public class ReportePanel extends JPanel {

    private final ReporteController controller;
    private final JLabel lblFecha = new JLabel();
    private final JLabel kpiPedidos = kpi();
    private final JLabel kpiVendido = kpi();
    private final JLabel kpiTicket = kpi();
    private final JLabel kpiTiempo = kpi();
    private final JLabel kpiPersonalizados = kpi();
    private final JPanel panelEstados = new JPanel(new GridLayout(1, EstadoPedido.values().length, 8, 8));

    public ReportePanel(ReporteController controller) {
        super(new BorderLayout(10, 10));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel norte = new JPanel(new BorderLayout());
        JLabel titulo = new JLabel("Indicadores del día");
        titulo.setFont(Estilo.TITULO);
        titulo.setForeground(Estilo.AZUL);
        norte.add(titulo, BorderLayout.WEST);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btn = Estilo.boton("Actualizar");
        btn.addActionListener(e -> refrescar());
        derecha.add(lblFecha);
        derecha.add(btn);
        norte.add(derecha, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(1, 5, 10, 10));
        tarjetas.add(tarjeta("Pedidos registrados", kpiPedidos));
        tarjetas.add(tarjeta("Total vendido", kpiVendido));
        tarjetas.add(tarjeta("Ticket promedio", kpiTicket));
        tarjetas.add(tarjeta("Tiempo prom. hasta 'Listo'", kpiTiempo));
        tarjetas.add(tarjeta("Ítems personalizados", kpiPersonalizados));

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.add(tarjetas, BorderLayout.NORTH);
        panelEstados.setBorder(Estilo.marco("Pedidos por estado"));
        panelEstados.setPreferredSize(new java.awt.Dimension(100, 130));
        centro.add(panelEstados, BorderLayout.CENTER);
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.add(centro, BorderLayout.NORTH);
        add(envoltura, BorderLayout.CENTER);

        JLabel origen = new JLabel("Origen de datos: " + controller.origenDatos());
        origen.setForeground(Color.GRAY);
        add(origen, BorderLayout.SOUTH);
        refrescar();
    }

    private static JLabel kpi() {
        JLabel l = new JLabel("-", SwingConstants.CENTER);
        l.setFont(Estilo.TOTAL);
        l.setForeground(Estilo.AZUL);
        return l;
    }

    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, Estilo.NARANJA),
                BorderFactory.createEmptyBorder(14, 10, 14, 10)));
        JLabel t = new JLabel(titulo, SwingConstants.CENTER);
        t.setForeground(Color.DARK_GRAY);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    public void refrescar() {
        try {
            ResumenVentas r = controller.resumenDelDia();
            lblFecha.setText(r.fecha().format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy",
                    Locale.forLanguageTag("es-PE"))) + "   ");
            kpiPedidos.setText(String.valueOf(r.pedidos()));
            kpiVendido.setText(Estilo.soles(r.totalVendido()));
            kpiTicket.setText(Estilo.soles(r.ticketPromedio()));
            kpiTiempo.setText(r.minutosPromedioAtencion() == null ? "—"
                    : String.format(Locale.ROOT, "%.1f min", r.minutosPromedioAtencion()));
            kpiPersonalizados.setText(String.format(Locale.ROOT, "%.0f %%", r.porcentajePersonalizados()));
            panelEstados.removeAll();
            for (EstadoPedido e : EstadoPedido.values()) {
                JLabel l = new JLabel("<html><center>" + e.getEtiqueta() + "<br><font size='+2'><b>"
                        + r.pedidosPorEstado().getOrDefault(e, 0) + "</b></font></center></html>",
                        SwingConstants.CENTER);
                l.setOpaque(true);
                l.setBackground(Estilo.AZUL_CLARO);
                panelEstados.add(l);
            }
            panelEstados.revalidate();
            panelEstados.repaint();
        } catch (AccesoDatosException ex) {
            lblFecha.setText("Sin conexión con la base de datos   ");
        }
    }
}
