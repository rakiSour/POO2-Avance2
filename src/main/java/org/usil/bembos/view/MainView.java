package org.usil.bembos.view;

import org.usil.bembos.controller.CocinaController;
import org.usil.bembos.controller.PagoController;
import org.usil.bembos.controller.PedidoController;
import org.usil.bembos.controller.ReporteController;
import org.usil.bembos.model.dao.DAOFactory;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/** Ventana principal: toma de pedidos, cocina (KDS) y reportes. */
public class MainView extends JFrame {

    private final JTabbedPane pestanas = new JTabbedPane();

    public MainView(PedidoController pedidoController, PagoController pagoController,
                    CocinaController cocinaController, ReporteController reporteController,
                    DAOFactory.TipoAlmacenamiento almacenamiento) {
        super("Bembos · Sistema de Gestión de Pedidos (MVC + Builder)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));

        CocinaPanel cocina = new CocinaPanel(cocinaController);
        ReportePanel reportes = new ReportePanel(reporteController);
        PedidoPanel pedidos = new PedidoPanel(pedidoController, pagoController, () -> {
            cocina.refrescar();
            reportes.refrescar();
        });

        pestanas.setFont(Estilo.SUBTITULO);
        pestanas.addTab("  Toma de pedido  ", pedidos);
        pestanas.addTab("  Cocina (KDS)  ", cocina);
        pestanas.addTab("  Reportes  ", reportes);
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == cocina) cocina.refrescar();
            if (pestanas.getSelectedComponent() == reportes) reportes.refrescar();
        });

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(encabezado(), BorderLayout.NORTH);
        raiz.add(pestanas, BorderLayout.CENTER);
        raiz.add(barraEstado(almacenamiento), BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    public JTabbedPane getPestanas() {
        return pestanas;
    }

    private static JPanel encabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Estilo.AZUL);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel marca = new JLabel("BEMBOS  ·  Gestión de Pedidos");
        marca.setFont(Estilo.TITULO);
        marca.setForeground(Color.WHITE);
        JLabel sub = new JLabel(":D  ");
        sub.setForeground(Estilo.NARANJA);
        p.add(marca, BorderLayout.WEST);
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    private static JPanel barraEstado(DAOFactory.TipoAlmacenamiento almacenamiento) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        String texto = almacenamiento == DAOFactory.TipoAlmacenamiento.POSTGRESQL
                ? "● Conectado a PostgreSQL (esquema bembos)  "
                : "● Modo demostración: datos en memoria (no se guardan al cerrar)";
        JLabel l = new JLabel(texto);
        l.setForeground(almacenamiento == DAOFactory.TipoAlmacenamiento.POSTGRESQL ? Estilo.VERDE : Estilo.NARANJA);
        p.add(l, BorderLayout.WEST);
        p.add(new JLabel("OdTech "), BorderLayout.EAST);
        return p;
    }
}
