package foro;

import auth.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel visual para mostrar la información de una idea en el muro.
 * Incluye título, autor, contenido, calificación, imagen y botones de acción.
 */
public class IdeaPanel extends JPanel {

    private final Idea idea;
    private final IdeaService ideaService;

    /**
     * Crea un nuevo panel para mostrar una idea.
     * @param idea Idea a mostrar.
     * @param usuarioActual Usuario actual (para permisos y votación).
     * @param ideaService Servicio de ideas.
     */
    public IdeaPanel(Idea idea, Usuario usuarioActual, IdeaService ideaService) {
        this.idea = idea;
        this.ideaService = ideaService;
        
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 0, 10, 0),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(20, 20, 20, 20)
            )
        ));
        setBackground(Color.WHITE);

        JPanel panelIzquierdo = new JPanel(new BorderLayout(0, 20));
        panelIzquierdo.setOpaque(false);

        JPanel topTextPanel = new JPanel();
        topTextPanel.setLayout(new BoxLayout(topTextPanel, BoxLayout.Y_AXIS));
        topTextPanel.setOpaque(false);

        JLabel tituloLabel = new JLabel(idea.getTitulo());
        tituloLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tituloLabel.setForeground(new Color(150, 0, 0));
        topTextPanel.add(tituloLabel);
        
        topTextPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JLabel autorLabel = new JLabel("Por: " + idea.getNombreEstudiante() + " (" + idea.getCodigoEstudiante() + ")");
        autorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        autorLabel.setForeground(new Color(150, 150, 150));
        topTextPanel.add(autorLabel);

        topTextPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JTextArea contenidoArea = new JTextArea(idea.getContenido());
        contenidoArea.setEditable(false);
        contenidoArea.setLineWrap(true);
        contenidoArea.setWrapStyleWord(true);
        contenidoArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contenidoArea.setOpaque(false);
        topTextPanel.add(contenidoArea);

        panelIzquierdo.add(topTextPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setOpaque(false);
        
        if (usuarioActual != null && !usuarioActual.getUsername().equals("admin")) {
            bottomPanel.add(new StarRatingPanel(idea, usuarioActual, ideaService));
        } else {
            double avg = idea.getAverageRating();
            String avgText = String.format("Calificación: %.1f ★ (%d votos)", avg, idea.getVoteCount());
            JLabel avgLabel = new JLabel(avgText);
            avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            bottomPanel.add(avgLabel);
        }
        
        bottomPanel.add(Box.createHorizontalGlue());

        JButton verDetallesButton = new JButton("Comentarios");
        verDetallesButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        verDetallesButton.setBackground(new Color(204, 0, 0));
        verDetallesButton.setForeground(Color.WHITE);
        verDetallesButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        
        verDetallesButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            VentanaDetalleIdea dialog = new VentanaDetalleIdea(owner, idea, ideaService, usuarioActual);
            dialog.setVisible(true);
        });
        bottomPanel.add(verDetallesButton);
        bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        if (usuarioActual != null && usuarioActual.getUsername().equals("admin")) {
            JButton deleteButton = new JButton("Eliminar Idea");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            deleteButton.addActionListener(e -> eliminarIdea());
            bottomPanel.add(deleteButton);
        }
        
        panelIzquierdo.add(bottomPanel, BorderLayout.SOUTH);
        add(panelIzquierdo, BorderLayout.CENTER);

        if (idea.getImageUrl() != null && !idea.getImageUrl().isEmpty()) {
            JLabel imageLabel = new JLabel("Cargando imagen...", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 200));
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(imageLabel, BorderLayout.EAST);
            ImageIcon icon = idea.getImageIcon(this);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imageLabel.setText(null);
            }
        }
    }
    
    /**
     * Elimina la idea actual tras confirmación.
     */
    private void eliminarIdea() {
        int confirm = JOptionPane.showConfirmDialog(
            this, "¿Estás seguro de que deseas eliminar esta idea permanentemente?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            ideaService.eliminarIdea(this.idea);
            Container parentContainer = this.getParent();
            if (parentContainer != null) {
                parentContainer.remove(this);
                parentContainer.revalidate();
                parentContainer.repaint();
            }
        }
    }
}
