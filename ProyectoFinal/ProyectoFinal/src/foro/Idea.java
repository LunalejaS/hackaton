package foro;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Representa una idea publicada en el foro.
 * Incluye información del estudiante, contenido, imagen, estado, votos y comentarios.
 */
public class Idea implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Estado { APROBADA, PENDIENTE, DESAPROBADA }

    private String nombreEstudiante;
    private String codigoEstudiante;
    private String titulo;
    private String contenido;
    private String imageUrl;
    private Estado estado;
    private Map<String, Integer> votes;
    private List<Comentario> comentarios;

    // Campos transitorios para la imagen cargada
    private transient ImageIcon loadedImage;
    private transient boolean isLoading = false;

    /**
     * Crea una nueva idea.
     * @param nombreEstudiante Nombre del estudiante.
     * @param codigoEstudiante Código del estudiante.
     * @param titulo Título de la idea.
     * @param contenido Contenido de la idea.
     * @param imageUrl URL de la imagen asociada (opcional).
     */
    public Idea(String nombreEstudiante, String codigoEstudiante, String titulo, String contenido, String imageUrl) {
        this.nombreEstudiante = nombreEstudiante;
        this.codigoEstudiante = codigoEstudiante;
        this.titulo = titulo;
        this.contenido = contenido;
        this.imageUrl = imageUrl;
        this.estado = Estado.PENDIENTE;
        this.votes = new ConcurrentHashMap<>();
        this.comentarios = new ArrayList<>();
    }

    // Métodos para comentarios
    public List<Comentario> getComentarios() {
        if (comentarios == null) comentarios = new ArrayList<>();
        return comentarios;
    }
    public void agregarComentario(Comentario comentario) { getComentarios().add(comentario); }
    public void eliminarComentario(Comentario comentario) { getComentarios().remove(comentario); }

    // Getters de campos principales
    public String getNombreEstudiante() { return nombreEstudiante; }
    public String getCodigoEstudiante() { return codigoEstudiante; }
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getImageUrl() { return imageUrl; }
    public Estado getEstado() { return estado; }
    public void aprobar() { this.estado = Estado.APROBADA; }
    public void desaprobar() { this.estado = Estado.DESAPROBADA; }

    // Métodos de votación
    private Map<String, Integer> getVotesMap() {
        if (this.votes == null) this.votes = new ConcurrentHashMap<>();
        return this.votes;
    }
    public void addVote(String username, int rating) {
        if (username != null && !username.isEmpty()) getVotesMap().put(username, rating);
    }
    public int getUserVote(String username) { return getVotesMap().getOrDefault(username, 0); }
    public int getVoteCount() { return getVotesMap().size(); }
    public double getAverageRating() {
        if (getVotesMap().isEmpty()) return 0.0;
        double sum = 0;
        for (int rating : getVotesMap().values()) sum += rating;
        return sum / getVotesMap().size();
    }

    /**
     * Carga y devuelve el icono de la imagen asociada a la idea, escalada.
     * @param componentToRepaint Componente que debe repintarse al cargar la imagen.
     * @return ImageIcon escalado o null si aún no está cargado.
     */
    public ImageIcon getImageIcon(Component componentToRepaint) {
        if (loadedImage != null) return loadedImage;
        if (isLoading || imageUrl == null || imageUrl.trim().isEmpty()) return null;
        isLoading = true;

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);
                if (image == null) throw new IOException("No se pudo leer la imagen desde la URL.");
                int originalWidth = image.getWidth(null);
                int originalHeight = image.getHeight(null);
                int maxWidth = 200, maxHeight = 200;
                int newWidth = originalWidth, newHeight = originalHeight;
                if (originalWidth > maxWidth) {
                    newWidth = maxWidth;
                    newHeight = (newWidth * originalHeight) / originalWidth;
                }
                if (newHeight > maxHeight) {
                    newHeight = maxHeight;
                    newWidth = (newHeight * originalWidth) / originalHeight;
                }
                return new ImageIcon(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));
            }
            @Override
            protected void done() {
                try { loadedImage = get(); } catch (Exception ignored) {}
                finally {
                    isLoading = false;
                    componentToRepaint.repaint();
                }
            }
        }.execute();

        return null;
    }

    public String getDescripcion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDescripcion'");
    }
}
