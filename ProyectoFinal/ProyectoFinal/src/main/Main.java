package main;

import auth.AuthService;
import auth.Usuario;
import foro.Foro;
import foro.IdeaService;

import javax.swing.*;
import java.awt.*;

/**
 * Clase principal que inicia y ejecuta la aplicación "UD FORO".
 * <p>
 * Esta clase configura el estilo visual (Look & Feel), instancia los servicios necesarios
 * y controla el flujo de interacción entre usuarios y el sistema a través de cuadros de diálogo.
 * Permite a los usuarios registrarse, iniciar sesión, enviar ideas, ver el muro global y
 * gestionar ideas según su rol (administrador o estudiante).
 */
public class Main {

    /**
     * Punto de entrada principal de la aplicación.
     * <p>
     * Configura la apariencia de la interfaz, inicializa servicios y
     * presenta un menú dinámico basado en el tipo de usuario (sin sesión, estudiante o administrador).
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        

        // --- INICIO: Configuración del estilo visual (Nimbus con personalización de colores) ---

        Color rojoPrincipal = new Color(204, 0, 0);
        Color blancoFondo = new Color(250, 250, 250);
        Color grisTexto = new Color(60, 60, 60);
        Color rojoFocus = new Color(230, 50, 50);

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Si Nimbus no está disponible, se usa el L&F por defecto
        }

        // Personalización de colores y fuentes
        UIManager.put("control", blancoFondo);
        UIManager.put("nimbusLightBackground", blancoFondo);
        UIManager.put("Panel.background", blancoFondo);
        UIManager.put("OptionPane.background", blancoFondo);
        UIManager.put("text", grisTexto);
        UIManager.put("OptionPane.messageForeground", grisTexto);
        UIManager.put("Button.background", rojoPrincipal);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("nimbusFocus", rojoFocus);
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(rojoPrincipal, 2, true),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 16));
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 18));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 16));
        UIManager.put("PasswordField.font", new Font("Segoe UI", Font.PLAIN, 16));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 16));

        // Logo personalizado para usar en el menú principal
        Icon logo = new LogoIcon(64);

        // --- FIN configuración estética ---

        // Instancias principales del sistema
        AuthService authService = new AuthService();
        IdeaService ideaService = new IdeaService();
        Foro foro = new Foro(ideaService);

        Usuario usuarioActual = null;
        boolean salir = false;

        // Bucle principal de la aplicación
        while (!salir) {
            // Determina el título del menú según el estado de sesión
            String tituloSesion = (usuarioActual != null)
                    ? "<html>Iniciaste sesión como <b>" + usuarioActual.getUsername() + "</b></html>"
                    : "Bienvenido a UD FORO";

            // Define las opciones según el rol del usuario
            String[] opciones;
            if (usuarioActual == null) {
                opciones = new String[]{"Registrarse", "Iniciar sesión", "Ver muro de ideas", "Salir"};
            } else if (usuarioActual.getUsername().equals("admin")) {
                opciones = new String[]{"Ver muro de ideas", "Gestionar ideas", "Cerrar sesión", "Salir"};
            } else {
                opciones = new String[]{"Ver muro de ideas", "Enviar idea", "Cerrar sesión", "Salir"};
            }

            // Muestra el menú principal
            int opcion = JOptionPane.showOptionDialog(
                    null,
                    tituloSesion,
                    "Menú Principal",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    logo,
                    opciones,
                    opciones[0]
            );

            // Control de flujo según el estado de sesión y opción seleccionada
            if (usuarioActual == null) {
                switch (opcion) {
                    case 0: // Registrarse
                        String[] registro = DialogUtils.pedirUsuarioYContrasena("Registro");
                        if (registro != null) {
                            String username = registro[0];
                            String password = registro[1];
                            if (authService.registrarUsuario(username, password)) {
                                JOptionPane.showMessageDialog(null,
                                        "<html><div style='font-size:16px;'>Registro exitoso. Ahora puede iniciar sesión.</div></html>");
                            } else {
                                JOptionPane.showMessageDialog(null,
                                        "<html><div style='font-size:16px;'>El usuario ya existe.</div></html>");
                            }
                        }
                        break;
                    case 1: // Iniciar sesión
                        String[] login = DialogUtils.pedirUsuarioYContrasena("Inicio de Sesión");
                        if (login != null) {
                            String username = login[0];
                            String password = login[1];
                            usuarioActual = authService.iniciarSesion(username, password);
                            if (usuarioActual != null) {
                                JOptionPane.showMessageDialog(null,
                                        "<html><div style='font-size:16px;'>Inicio de sesión exitoso. ¡Bienvenido, " + username + "!</div></html>");
                            } else {
                                JOptionPane.showMessageDialog(null,
                                        "<html><div style='font-size:16px;'>Usuario o contraseña incorrectos.</div></html>");
                            }
                        }
                        break;
                    case 2: // Ver muro de ideas (modo visitante)
                        foro.mostrarMuroGlobalIdeas(usuarioActual);
                        break;
                    case 3: // Salir
                    case JOptionPane.CLOSED_OPTION:
                        salir = true;
                        break;
                }
            } else if (usuarioActual.getUsername().equals("admin")) {
                switch (opcion) {
                    case 0: // Ver muro global (admin)
                        foro.mostrarMuroGlobalIdeas(usuarioActual);
                        break;
                    case 1: // Gestionar ideas pendientes
                        foro.mostrarIdeasPendientes();
                        break;
                    case 2: // Cerrar sesión
                        usuarioActual = null;
                        JOptionPane.showMessageDialog(null,
                                "<html><div style='font-size:16px;'>Sesión cerrada.</div></html>");
                        break;
                    case 3: // Salir
                    case JOptionPane.CLOSED_OPTION:
                        salir = true;
                        break;
                }
            } else { // Usuario estudiante
                switch (opcion) {
                    case 0: // Ver muro de ideas
                        foro.mostrarMuroGlobalIdeas(usuarioActual);
                        break;
                    case 1: // Enviar idea
                        String[] datosIdea = IdeaInputDialog.mostrarDialogo();
                        if (datosIdea != null) {
                            foro.agregarIdea(datosIdea[0], datosIdea[1], datosIdea[2], datosIdea[3], datosIdea[4]);
                        }
                        break;
                    case 2: // Cerrar sesión
                        usuarioActual = null;
                        JOptionPane.showMessageDialog(null,
                                "<html><div style='font-size:16px;'>Sesión cerrada.</div></html>");
                        break;
                    case 3: // Salir
                    case JOptionPane.CLOSED_OPTION:
                        salir = true;
                        break;
                }
            }
        }
    }
}
