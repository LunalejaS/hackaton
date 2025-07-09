package foro;

import auth.Usuario;
import javax.swing.*;
import java.awt.*;

/**
 * {@code IdeaListCellRenderer} es un renderizador personalizado para listas de objetos {@link Idea}.
 * Su propósito es mostrar cada elemento de la lista como un panel visual estilizado mediante la clase {@link IdeaPanel},
 * permitiendo una representación rica y contextual de cada idea.
 * <p>
 * Este componente se utiliza principalmente en interfaces donde se desea listar múltiples ideas en un {@link JList},
 * como en el muro global del foro.
 *
 * <p><b>Aplicación del PDF - Categoría 3: Eventos Sociales y Académicos de la Carrera:</b><br>
 * Esta clase contribuye a la presentación visual de las ideas propuestas por los estudiantes, facilitando
 * su lectura y evaluación en el muro global. Esto responde al objetivo del hackathon de mejorar la interacción
 * entre los miembros de la comunidad académica mediante herramientas que visualizan, discuten y votan ideas relacionadas
 * con eventos académicos y sociales.
 */
public class IdeaListCellRenderer implements ListCellRenderer<Idea> {
    
    /** Usuario actual que está visualizando la lista de ideas */
    private final Usuario usuarioActual;

    /** Servicio que administra las ideas (para operaciones de voto, eliminación, etc.) */
    private final IdeaService ideaService;
    
    /**
     * Constructor del renderizador de celdas para ideas.
     *
     * @param usuarioActual El usuario actualmente autenticado (puede ser admin o estudiante).
     * @param ideaService Servicio que gestiona el almacenamiento y actualización de ideas.
     */
    public IdeaListCellRenderer(Usuario usuarioActual, IdeaService ideaService) {
        this.usuarioActual = usuarioActual;
        this.ideaService = ideaService;
    }

    /**
     * Este método crea un componente visual que representa una idea dentro de una celda del {@link JList}.
     * Utiliza {@link IdeaPanel} para construir el componente y adapta su apariencia si está seleccionada.
     *
     * @param list La lista que contiene las ideas.
     * @param idea El objeto {@link Idea} actual a renderizar.
     * @param index Índice del elemento en la lista.
     * @param isSelected {@code true} si la celda está seleccionada.
     * @param cellHasFocus {@code true} si la celda tiene el foco del teclado.
     * @return El componente visual que representa esta celda.
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends Idea> list, Idea idea, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        // Crea el panel visual personalizado con la información de la idea
        IdeaPanel panel = new IdeaPanel(idea, usuarioActual, ideaService);

        // Cambia el fondo si está seleccionada para mayor contraste visual
        if (isSelected) {
            panel.setBackground(new Color(220, 235, 255)); // Azul claro
        } else {
            panel.setBackground(Color.WHITE); // Fondo por defecto
        }

        return panel;
    }
}
