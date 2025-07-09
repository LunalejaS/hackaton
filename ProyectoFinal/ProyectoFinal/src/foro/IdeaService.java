package foro;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar la persistencia y operaciones sobre las ideas del foro.
 * Permite agregar, aprobar, desaprobar, eliminar y obtener ideas.
 */
public class IdeaService {
    private List<Idea> ideas = new ArrayList<>();
    private static final String ARCHIVO_IDEAS = "ideas.dat";

    /**
     * Constructor que carga las ideas desde archivo.
     */
    public IdeaService() { cargarIdeas(); }

    /**
     * Devuelve la lista completa de ideas.
     */
    public List<Idea> getIdeas() { return ideas; }

    /**
     * Devuelve la lista de ideas pendientes de aprobación.
     */
    public List<Idea> getIdeasPendientes() {
        List<Idea> pendientes = new ArrayList<>();
        for (Idea idea : ideas) {
            if (idea.getEstado() == Idea.Estado.PENDIENTE) pendientes.add(idea);
        }
        return pendientes;
    }

    /**
     * Devuelve la lista de ideas aprobadas.
     */
    public List<Idea> getIdeasAprobadas() {
        List<Idea> aprobadas = new ArrayList<>();
        for (Idea idea : ideas) {
            if (idea.getEstado() == Idea.Estado.APROBADA) aprobadas.add(idea);
        }
        return aprobadas;
    }

    /**
     * Agrega una nueva idea y la guarda en archivo.
     */
    public void agregarIdea(Idea idea) {
        ideas.add(idea);
        guardarIdeas();
    }

    /**
     * Marca una idea como aprobada y guarda los cambios.
     */
    public void aprobarIdea(Idea idea) {
        idea.aprobar();
        guardarIdeas();
    }

    /**
     * Marca una idea como desaprobada y guarda los cambios.
     */
    public void desaprobarIdea(Idea idea) {
        idea.desaprobar();
        guardarIdeas();
    }

    /**
     * Elimina una idea del sistema y guarda los cambios.
     */
    public void eliminarIdea(Idea idea) {
        ideas.remove(idea);
        guardarIdeas();
    }

    /**
     * Guarda la lista de ideas en el archivo de persistencia.
     */
    public void guardarIdeas() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_IDEAS))) {
            oos.writeObject(ideas);
        } catch (IOException e) {
            guardarErrorEnTxt("Error guardando ideas: " + e.getMessage());
        }
    }

    /**
     * Carga la lista de ideas desde el archivo de persistencia.
     */
    @SuppressWarnings("unchecked")
    private void cargarIdeas() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARCHIVO_IDEAS))) {
            ideas = (List<Idea>) ois.readObject();
        } catch (Exception e) {
            ideas = new ArrayList<>();
            guardarErrorEnTxt("Error cargando ideas: " + e.getMessage());
        }
    }

    /**
     * Registra mensajes de error en un archivo de texto local.
     * @param mensaje Mensaje de error a guardar.
     */
    private void guardarErrorEnTxt(String mensaje) {
        try (FileWriter fw = new FileWriter("errores_serializacion.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(java.time.LocalDateTime.now() + " - " + mensaje);
        } catch (IOException ex) {
            // No se puede hacer más si falla el registro de errores
        }
    }
}
