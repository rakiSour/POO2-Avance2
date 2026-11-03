package org.usil.bembos;

import org.usil.bembos.config.ConexionBD;
import org.usil.bembos.controller.CocinaController;
import org.usil.bembos.controller.PagoController;
import org.usil.bembos.controller.PedidoController;
import org.usil.bembos.controller.ReporteController;
import org.usil.bembos.model.dao.DAOFactory;
import org.usil.bembos.model.servicio.PedidoFacade;
import org.usil.bembos.view.Estilo;
import org.usil.bembos.view.MainView;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Arrays;

/**
 * Punto de entrada. Ensambla las capas MVC:
 * Modelo (DAOFactory + PedidoFacade) -> Controladores -> Vistas Swing.
 * <p>
 * Argumentos opcionales:
 * <ul>
 *   <li>{@code --memoria}: inicia sin PostgreSQL (datos en memoria).</li>
 *   <li>{@code --web}: modo navegador (CheerpJ/Vercel): memoria, ventana
 *       maximizada y sin cerrar la maquina virtual al cerrar la ventana.</li>
 * </ul>
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        boolean web = Arrays.asList(args).contains("--web");
        boolean memoria = web || Arrays.asList(args).contains("--memoria");
        SwingUtilities.invokeLater(() -> iniciar(memoria, web));
    }

    private static void iniciar(boolean forzarMemoria, boolean web) {
        Estilo.aplicarLookAndFeel();
        DAOFactory.TipoAlmacenamiento tipo = elegirAlmacenamiento(forzarMemoria);
        if (tipo == null) {
            System.exit(0);
            return;
        }
        DAOFactory daoFactory = DAOFactory.crear(tipo);
        PedidoFacade facade = PedidoFacade.crear(daoFactory);

        MainView vista = new MainView(
                new PedidoController(facade),
                new PagoController(facade),
                new CocinaController(facade),
                new ReporteController(facade),
                tipo);
        if (web) {
            // En el navegador la ventana ocupa todo el lienzo y no debe terminar la JVM.
            vista.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
            vista.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        } else if (tipo == DAOFactory.TipoAlmacenamiento.POSTGRESQL) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> ConexionBD.getInstancia().cerrarTodo()));
        }
        vista.setVisible(true);
    }

    private static DAOFactory.TipoAlmacenamiento elegirAlmacenamiento(boolean forzarMemoria) {
        if (forzarMemoria) {
            return DAOFactory.TipoAlmacenamiento.MEMORIA;
        }
        ConexionBD bd = ConexionBD.getInstancia();
        if (bd.probarConexion()) {
            return DAOFactory.TipoAlmacenamiento.POSTGRESQL;
        }
        int opcion = JOptionPane.showConfirmDialog(null,
                "<html>No fue posible conectarse a PostgreSQL:<br><code>" + bd.getUrl() + "</code><br><br>"
                        + "Verifique que el servicio esté iniciado, que exista la base <b>bembos_db</b><br>"
                        + "(scripts en <i>src/main/resources/db</i>) y el usuario/clave de <i>db.properties</i>.<br><br>"
                        + "¿Desea continuar en <b>modo demostración</b> (datos en memoria)?</html>",
                "Base de datos no disponible", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return opcion == JOptionPane.YES_OPTION ? DAOFactory.TipoAlmacenamiento.MEMORIA : null;
    }
}
