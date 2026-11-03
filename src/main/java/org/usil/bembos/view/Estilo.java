package org.usil.bembos.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Paleta, tipografias y utilidades comunes de la Vista. */
public final class Estilo {

    public static final Color AZUL = new Color(0x0B3B8C);
    public static final Color AZUL_CLARO = new Color(0xE8EEF9);
    public static final Color NARANJA = new Color(0xF28C00);
    public static final Color VERDE = new Color(0x2E7D32);
    public static final Color ROJO = new Color(0xC62828);
    public static final Color FONDO = new Color(0xF6F7FB);
    public static final Color AMARILLO_SUAVE = new Color(0xFFF4D6);
    public static final Color VERDE_SUAVE = new Color(0xE3F4E5);

    public static final Font TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font SUBTITULO = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font NORMAL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font TOTAL = new Font(Font.SANS_SERIF, Font.BOLD, 22);

    private static final NumberFormat SOLES = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    private Estilo() {
    }

    public static void aplicarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // se mantiene el look and feel por defecto
        }
        UIManager.put("nimbusBase", AZUL);
        UIManager.put("nimbusSelectionBackground", AZUL);
        UIManager.put("control", FONDO);
        UIManager.put("defaultFont", NORMAL);
    }

    public static String soles(BigDecimal monto) {
        return SOLES.format(monto == null ? BigDecimal.ZERO : monto);
    }

    public static JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(NARANJA);
        b.setForeground(Color.WHITE);
        b.setFont(SUBTITULO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(SUBTITULO);
        l.setForeground(AZUL);
        return l;
    }

    public static Border marco(String titulo) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(AZUL_CLARO.darker()), titulo,
                        0, 0, SUBTITULO, AZUL),
                BorderFactory.createEmptyBorder(6, 8, 8, 8));
    }

    public static void mostrarErrores(Component padre, String titulo, List<String> errores) {
        StringBuilder sb = new StringBuilder("<html><body style='width:340px'>");
        for (String e : errores) {
            sb.append("• ").append(e).append("<br>");
        }
        sb.append("</body></html>");
        JOptionPane.showMessageDialog(padre, sb.toString(), titulo, JOptionPane.WARNING_MESSAGE);
    }

    public static void mostrarError(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
