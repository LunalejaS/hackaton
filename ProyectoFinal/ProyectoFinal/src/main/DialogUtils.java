package main;

import javax.swing.*;
import java.awt.*;

/**
 * {@code DialogUtils} es una clase utilitaria que proporciona métodos para mostrar
 * cuadros de diálogo personalizados relacionados con el ingreso de credenciales de usuario.
 * <p>
 * Actualmente contiene un único método estático para pedir al usuario un nombre de usuario
 * y una contraseña con una interfaz más amigable que un {@code JOptionPane} estándar.
 */
public class DialogUtils {

    /**
     * Muestra un cuadro de diálogo personalizado para solicitar un nombre de usuario y una contraseña.
     * <p>
     * Utiliza componentes personalizados como campos de texto, etiquetas y un diseño con {@link GridBagLayout}
     * para asegurar una presentación visual ordenada y consistente.
     *
     * @param titulo Título que se mostrará en la ventana del cuadro de diálogo.
     * @return Un arreglo de dos cadenas: la primera contiene el nombre de usuario y la segunda la contraseña;
     *         o {@code null} si el usuario cancela la operación.
     */
    public static String[] pedirUsuarioYContrasena(String titulo) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta y campo para el nombre de usuario
        JLabel usuarioLabel = new JLabel("Usuario:");
        usuarioLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JTextField usuarioField = new JTextField();
        usuarioField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usuarioField.setPreferredSize(new Dimension(200, 30));

        // Etiqueta y campo para la contraseña
        JLabel contrasenaLabel = new JLabel("Contraseña:");
        contrasenaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JPasswordField contrasenaField = new JPasswordField();
        contrasenaField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contrasenaField.setPreferredSize(new Dimension(200, 30));

        // Posicionamiento en el panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(usuarioLabel, gbc);
        gbc.gridx = 1;
        panel.add(usuarioField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(contrasenaLabel, gbc);
        gbc.gridx = 1;
        panel.add(contrasenaField, gbc);

        panel.setPreferredSize(new Dimension(420, 130));

        // Mostrar el cuadro de diálogo
        int result = JOptionPane.showConfirmDialog(
                null, panel, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // Si el usuario acepta, se retornan los datos; si cancela, se retorna null
        if (result == JOptionPane.OK_OPTION) {
            String usuario = usuarioField.getText();
            String contrasena = new String(contrasenaField.getPassword());
            return new String[]{usuario, contrasena};
        } else {
            return null;
        }
    }
}
