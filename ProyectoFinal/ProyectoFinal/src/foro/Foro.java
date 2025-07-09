package foro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.List;
import auth.Usuario;

/**
 * Clase principal para la gestión y visualización del foro de ideas.
 * Permite mostrar el muro global, gestionar ideas pendientes y agregar nuevas ideas.
 */
public class Foro {
    private final IdeaService ideaService;

    /**
     * Constructor que recibe el servicio de ideas.
     * @param ideaService Servicio para gestionar ideas.
     */
    public Foro(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    /**
     * Muestra el muro global de ideas aprobadas.
     * @param usuarioActual Usuario que visualiza el muro (puede ser nulo).
     */
    public void mostrarMuroGlobalIdeas(Usuario usuarioActual) {
        List<Idea> ideasAprobadas = ideaService.getIdeasAprobadas();
        if (ideasAprobadas.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "<html><div style='font-size:16px;'>No hay ideas aprobadas para mostrar.</div></html>",
                    "Muro Global de Ideas", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel ideasContainer = new JPanel();
        ideasContainer.setLayout(new BoxLayout(ideasContainer, BoxLayout.Y_AXIS));

        for (Idea idea : ideasAprobadas) {
            IdeaPanel panelDeIdea = new IdeaPanel(idea, usuarioActual, ideaService);
            panelDeIdea.setAlignmentX(Component.LEFT_ALIGNMENT);
            ideasContainer.add(panelDeIdea);
        }

        JScrollPane scrollPane = new JScrollPane(ideasContainer);
        scrollPane.setPreferredSize(new java.awt.Dimension(800, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JOptionPane.showMessageDialog(null, scrollPane, "Muro Global de Ideas", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Muestra la tabla de ideas pendientes para su revisión y gestión.
     */
    public void mostrarIdeasPendientes() {
        List<Idea> pendientes = ideaService.getIdeasPendientes();
        if (pendientes.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "<html><div style='font-size:16px;'>No hay ideas pendientes para revisar.</div></html>",
                    "Ideas Pendientes",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new String[]{"Nombre", "Código", "Idea", "Aprobar", "Desaprobar", "Eliminar"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };

        for (Idea idea : pendientes) {
            model.addRow(new Object[]{
                idea.getNombreEstudiante(), 
                idea.getCodigoEstudiante(), 
                idea.getContenido(), 
                "Aprobar", 
                "Desaprobar", 
                "Eliminar"
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        table.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row < 0 || col < 3) return;

                List<Idea> pendientesActuales = ideaService.getIdeasPendientes();
                if (row >= pendientesActuales.size()) return;
                
                Idea idea = pendientesActuales.get(row);

                if (col == 3) { // Aprobar
                    ideaService.aprobarIdea(idea);
                    JOptionPane.showMessageDialog(null, "Idea aprobada.");
                } else if (col == 4) { // Desaprobar
                    ideaService.desaprobarIdea(idea);
                    JOptionPane.showMessageDialog(null, "Idea desaprobada.");
                } else if (col == 5) { // Eliminar
                    int confirm = JOptionPane.showConfirmDialog(null, 
                        "¿Estás seguro de que deseas eliminar esta idea permanentemente?", 
                        "Confirmar Eliminación", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.WARNING_MESSAGE);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        ideaService.eliminarIdea(idea);
                        JOptionPane.showMessageDialog(null, "Idea eliminada.");
                    } else {
                        return;
                    }
                }
                model.removeRow(row);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new java.awt.Dimension(800, 300));

        JOptionPane.showMessageDialog(null, scrollPane, "Ideas Pendientes", JOptionPane.PLAIN_MESSAGE);
    }
    
    /**
     * Agrega una nueva idea al foro y la envía para aprobación.
     * @param nombreEstudiante Nombre del estudiante.
     * @param codigoEstudiante Código del estudiante.
     * @param titulo Título de la idea.
     * @param contenidoIdea Contenido de la idea.
     * @param imageUrl URL de la imagen asociada (opcional).
     */
    public void agregarIdea(String nombreEstudiante, String codigoEstudiante, String titulo, String contenidoIdea, String imageUrl) {
        Idea nuevaIdea = new Idea(nombreEstudiante.trim(), codigoEstudiante.trim(), titulo.trim(), contenidoIdea.trim(), imageUrl.trim());
        ideaService.agregarIdea(nuevaIdea);
        JOptionPane.showMessageDialog(null, "Idea enviada para aprobación del administrador.");
    }
}
