package foro;

import auth.Usuario;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * {@code StarRatingPanel} representa un componente gráfico que permite a los estudiantes
 * calificar una idea mediante una interfaz de estrellas (de 1 a 5).
 * <p>
 * Este panel solo permite la interacción si el usuario autenticado no es administrador.
 * Las estrellas cambian de color al pasar el cursor sobre ellas y se actualizan al hacer clic.
 * <p>
 * Los votos se almacenan por usuario y se actualizan automáticamente en el sistema mediante {@link IdeaService}.
 */
public class StarRatingPanel extends JPanel {

    /** Arreglo de etiquetas que representan las estrellas (visualmente) */
    private final JLabel[] stars = new JLabel[5];

    /** Idea asociada al panel de calificación */
    private final Idea idea;

    /** Usuario autenticado que realiza la calificación */
    private final Usuario usuario;

    /** Servicio encargado de guardar las ideas y sus calificaciones */
    private final IdeaService ideaService;

    /** Color utilizado para las estrellas seleccionadas */
    private final Color starColor = new Color(255, 204, 0); // Amarillo dorado

    /**
     * Constructor que inicializa el panel de calificación con estrellas para una idea dada.
     *
     * @param idea La idea que será calificada.
     * @param usuario El usuario que califica la idea.
     * @param ideaService Servicio que maneja la persistencia de las ideas y votos.
     */
    public StarRatingPanel(Idea idea, Usuario usuario, IdeaService ideaService) {
        this.idea = idea;
        this.usuario = usuario;
        this.ideaService = ideaService;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false); // Fondo transparente para integrarse con el diseño exterior

        for (int i = 0; i < 5; i++) {
            stars[i] = new JLabel("☆"); // Estrella vacía por defecto
            stars[i].setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
            stars[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cambia el cursor al pasar sobre la estrella
            stars[i].setForeground(Color.LIGHT_GRAY); // Color por defecto
            add(stars[i]);

            // Espaciado entre estrellas
            if (i < 4) {
                add(Box.createRigidArea(new Dimension(5, 0)));
            }

            final int rating = i + 1; // Calificación correspondiente a la estrella actual

            // Eventos del mouse sobre cada estrella
            stars[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Al hacer clic, se guarda el voto y se actualiza visualmente
                    idea.addVote(usuario.getUsername(), rating);
                    ideaService.guardarIdeas();
                    updateStars(rating);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    // Previsualización al pasar el mouse
                    updateStars(rating, true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // Restaurar la calificación real al quitar el mouse
                    updateStars(idea.getUserVote(usuario.getUsername()));
                }
            });
        }

        // Muestra las estrellas según el voto guardado (si lo hay)
        updateStars(idea.getUserVote(usuario.getUsername()));
    }

    /**
     * Actualiza las estrellas para reflejar una calificación dada.
     *
     * @param rating Calificación actual del usuario (1 a 5).
     */
    private void updateStars(int rating) {
        updateStars(rating, false);
    }

    /**
     * Cambia la apariencia de las estrellas según una calificación.
     * Si es previsualización (hover), se actualizan sin guardar.
     *
     * @param rating Número de estrellas a mostrar como seleccionadas.
     * @param isPreview Si es {@code true}, se trata de una vista previa (hover); si es {@code false}, es calificación real.
     */
    private void updateStars(int rating, boolean isPreview) {
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars[i].setText("★"); // Estrella llena
                stars[i].setForeground(starColor);
            } else {
                stars[i].setText("☆"); // Estrella vacía
                stars[i].setForeground(Color.LIGHT_GRAY);
            }
        }
    }
}
