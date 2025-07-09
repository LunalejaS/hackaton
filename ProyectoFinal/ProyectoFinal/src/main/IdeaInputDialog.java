package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 * {@code IdeaInputDialog} es una clase utilitaria que muestra un cuadro de diálogo interactivo
 * para que los estudiantes ingresen y envíen nuevas ideas al sistema.
 * <p>
 * El formulario incluye campos para nombre completo, código estudiantil, título, contenido
 * y una URL opcional de imagen. Además, implementa validaciones en tiempo real y muestra
 * una vista previa automática de la imagen cargada desde la URL.
 */
public class IdeaInputDialog {

    /** Temporizador para activar la vista previa de la imagen después de que el usuario deja de escribir */
    private static Timer urlTypingTimer;

    /**
     * Muestra el cuadro de diálogo para ingresar una nueva idea.
     *
     * @return Un arreglo de 5 cadenas con el formato:
     *         [nombre, código, título, contenido, imageUrl] si se completa correctamente;
     *         o {@code null} si el usuario cancela.
     */
    public static String[] mostrarDialogo() {
        // Campos de entrada
        JTextField nombreField = new JTextField();
        JTextField codigoField = new JTextField();
        JTextField tituloField = new JTextField();
        JTextArea contenidoArea = new JTextArea(5, 20);
        JTextField imageUrlField = new JTextField();
        JLabel previewLabel = new JLabel("Vista Previa", SwingConstants.CENTER);

        // Configuración visual de componentes
        contenidoArea.setLineWrap(true);
        contenidoArea.setWrapStyleWord(true);
        previewLabel.setPreferredSize(new Dimension(200, 150));
        previewLabel.setBorder(BorderFactory.createEtchedBorder());
        previewLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Color.WHITE);

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 16);
        nombreField.setFont(mainFont);
        codigoField.setFont(mainFont);
        tituloField.setFont(mainFont);
        contenidoArea.setFont(mainFont);
        imageUrlField.setFont(mainFont);

        // Panel principal
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel izquierdo: formulario textual
        JPanel textDataPanel = new JPanel();
        textDataPanel.setLayout(new BoxLayout(textDataPanel, BoxLayout.Y_AXIS));

        textDataPanel.add(new JLabel("Nombre completo:"));
        textDataPanel.add(nombreField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Código estudiantil:"));
        textDataPanel.add(codigoField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Título de la idea:"));
        textDataPanel.add(tituloField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Contenido de la idea:"));

        JScrollPane scrollPane = new JScrollPane(contenidoArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        textDataPanel.add(scrollPane);
        panel.add(textDataPanel, BorderLayout.CENTER);

        // Panel derecho: imagen y URL
        JPanel imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setPreferredSize(new Dimension(220, 250));
        imagePanel.add(new JLabel("URL de la imagen (opcional):"), BorderLayout.NORTH);
        imagePanel.add(imageUrlField, BorderLayout.CENTER);
        imagePanel.add(previewLabel, BorderLayout.SOUTH);
        panel.add(imagePanel, BorderLayout.EAST);

        // Inicialización del temporizador para vista previa de imagen
        urlTypingTimer = new Timer(500, e -> updateImagePreview(imageUrlField, previewLabel));
        urlTypingTimer.setRepeats(false);

        // Escuchadores del campo de URL para activar el temporizador
        imageUrlField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { urlTypingTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { urlTypingTimer.restart(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // Bucle de validación del formulario
        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Enviar Idea",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            urlTypingTimer.stop(); // Detener vista previa si se cierra

            if (result != JOptionPane.OK_OPTION) return null;

            String nombre = nombreField.getText().trim();
            String codigo = codigoField.getText().trim();
            String titulo = tituloField.getText().trim();
            String contenido = contenidoArea.getText().trim();
            String imageUrl = imageUrlField.getText().trim();

            ArrayList<String> errores = new ArrayList<>();
            if (nombre.isEmpty()) errores.add("• Nombre completo");
            if (!codigo.matches("\\d{11}")) errores.add("• Código estudiantil (11 dígitos numéricos)");
            if (titulo.isEmpty()) errores.add("• Título de la idea");
            if (contenido.isEmpty()) errores.add("• Contenido de la idea");

            if (errores.isEmpty()) {
                return new String[]{nombre, codigo, titulo, contenido, imageUrl};
            }

            // Mostrar advertencia con los campos incompletos
            JOptionPane.showMessageDialog(null,
                    "<html><div style='font-size:14px;'>Por favor completa los siguientes campos:<br>" +
                            String.join("<br>", errores) + "</div></html>",
                    "Campos incompletos", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Carga y muestra una vista previa de imagen desde una URL.
     * <p>
     * Si la URL no es válida o no apunta a una imagen, se muestra un mensaje de error.
     *
     * @param imageUrlField Campo de texto que contiene la URL ingresada.
     * @param previewLabel Etiqueta donde se mostrará la imagen cargada o el mensaje de error.
     */
    private static void updateImagePreview(JTextField imageUrlField, JLabel previewLabel) {
        String urlText = imageUrlField.getText().trim();
        if (urlText.isEmpty()) {
            previewLabel.setIcon(null);
            previewLabel.setText("Vista Previa");
            return;
        }

        previewLabel.setIcon(null);
        previewLabel.setText("Cargando...");

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL(urlText);
                Image image = ImageIO.read(url);
                if (image == null) throw new Exception("No es una imagen válida");
                return new ImageIcon(image.getScaledInstance(200, 150, Image.SCALE_SMOOTH));
            }

            @Override
            protected void done() {
                try {
                    ImageIcon imageIcon = get();
                    previewLabel.setText(null);
                    previewLabel.setIcon(imageIcon);
                } catch (Exception ex) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("<html><center>URL no válida o<br>imagen no encontrada</center></html>");
                }
            }
        }.execute();
    }
}
