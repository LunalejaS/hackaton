package foro;

import auth.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * {@code VentanaDetalleIdea} es un cuadro de diálogo modal que muestra los detalles completos de una idea,
 * incluyendo título, autor, contenido, imagen asociada (si existe) y una sección de comentarios.
 * <p>
 * Permite que cualquier usuario agregue comentarios y que el administrador pueda eliminar comentarios
 * existentes. El diseño está optimizado para ofrecer una lectura cómoda del contenido y la interacción con los comentarios.
 */
public class VentanaDetalleIdea extends JDialog {

    private final Idea idea;
    private final Usuario usuarioActual;
    private final IdeaService ideaService;
    private final JPanel listaComentariosPanel;

    /**
     * Crea una nueva ventana de detalle para visualizar una idea específica.
     *
     * @param owner La ventana propietaria de este diálogo.
     * @param idea La idea que se mostrará en detalle.
     * @param ideaService Servicio responsable de guardar cambios en las ideas (por ejemplo, nuevos comentarios).
     * @param usuarioActual El usuario autenticado actualmente (puede ser nulo o administrador).
     */
    public VentanaDetalleIdea(Window owner, Idea idea, IdeaService ideaService, Usuario usuarioActual) {
        super(owner, "Detalle de la Idea", ModalityType.APPLICATION_MODAL);
        
        this.idea = idea;
        this.ideaService = ideaService;
        this.usuarioActual = usuarioActual;

        setSize(750, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 20));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Panel superior: muestra título, autor, contenido e imagen
        JPanel panelIdea = new JPanel(new BorderLayout(10, 10));
        String textoCompleto = String.format(
            "<html><h1 style='color: rgb(150,0,0); margin-bottom: 2px;'>%s</h1>" +
            "<p style='color: gray; font-style: italic; margin-top: 0px;'>Por: %s (%s)</p><br><p>%s</p></html>",
            idea.getTitulo(), idea.getNombreEstudiante(), idea.getCodigoEstudiante(), idea.getContenido().replace("\n", "<br>")
        );
        JEditorPane editorPane = new JEditorPane("text/html", textoCompleto);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        panelIdea.add(editorPane, BorderLayout.CENTER);

        // Imagen asociada a la idea (si aplica)
        if (idea.getImageUrl() != null && !idea.getImageUrl().isEmpty()) {
            JLabel imageLabel = new JLabel("Cargando...", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 200));
            panelIdea.add(imageLabel, BorderLayout.EAST);
            ImageIcon icon = idea.getImageIcon(imageLabel);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imageLabel.setText(null);
            }
        }

        contentPanel.add(panelIdea, BorderLayout.NORTH);

        // Sección de comentarios
        JPanel seccionComentarios = new JPanel(new BorderLayout(10, 10));
        seccionComentarios.setBorder(BorderFactory.createTitledBorder("Comentarios"));

        listaComentariosPanel = new JPanel();
        listaComentariosPanel.setLayout(new BoxLayout(listaComentariosPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollComentarios = new JScrollPane(listaComentariosPanel);
        scrollComentarios.setBorder(null);
        seccionComentarios.add(scrollComentarios, BorderLayout.CENTER);

        // Campo para nuevo comentario
        JPanel panelNuevoComentario = new JPanel(new BorderLayout(5, 5));
        JTextField campoComentario = new JTextField();
        JButton botonComentar = new JButton("Comentar");

        // Acción del botón para agregar comentario
        botonComentar.addActionListener(e -> {
            String texto = campoComentario.getText().trim();
            if (!texto.isEmpty()) {
                String autor = (usuarioActual != null) ? usuarioActual.getUsername() : "Anónimo";
                idea.agregarComentario(new Comentario(autor, texto));
                ideaService.guardarIdeas();
                campoComentario.setText("");
                reconstruirListaComentarios();
            }
        });

        panelNuevoComentario.add(campoComentario, BorderLayout.CENTER);
        panelNuevoComentario.add(botonComentar, BorderLayout.EAST);
        seccionComentarios.add(panelNuevoComentario, BorderLayout.SOUTH);

        contentPanel.add(seccionComentarios, BorderLayout.CENTER);

        add(new JScrollPane(contentPanel));
        reconstruirListaComentarios();
    }

    /**
     * Reconstruye visualmente la lista de comentarios en la interfaz,
     * eliminando y redibujando todos los comentarios actuales.
     */
    private void reconstruirListaComentarios() {
        listaComentariosPanel.removeAll();
        for (Comentario comentario : idea.getComentarios()) {
            JPanel panelComentario = new JPanel(new BorderLayout(10, 2));
            panelComentario.setBorder(new EmptyBorder(5, 5, 5, 5));

            JLabel autorLabel = new JLabel(String.format(
                "<html><b>%s</b> <font color='gray'>(%s)</font></html>",
                comentario.getAutor(), comentario.getFechaFormateada()
            ));
            panelComentario.add(autorLabel, BorderLayout.NORTH);

            JTextArea textoArea = new JTextArea(comentario.getTexto());
            textoArea.setLineWrap(true);
            textoArea.setWrapStyleWord(true);
            textoArea.setEditable(false);
            panelComentario.add(textoArea, BorderLayout.CENTER);

            // Permitir que el administrador elimine comentarios
            if (usuarioActual != null && usuarioActual.getUsername().equals("admin")) {
                JButton botonEliminar = new JButton("X");
                botonEliminar.setForeground(Color.RED);
                botonEliminar.setMargin(new Insets(0, 4, 0, 4));
                botonEliminar.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "¿Eliminar este comentario?", "Confirmar",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        idea.eliminarComentario(comentario);
                        ideaService.guardarIdeas();
                        reconstruirListaComentarios();
                    }
                });
                panelComentario.add(botonEliminar, BorderLayout.EAST);
            }

            listaComentariosPanel.add(panelComentario);
        }

        listaComentariosPanel.revalidate();
        listaComentariosPanel.repaint();
    }
}
