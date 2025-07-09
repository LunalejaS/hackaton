package foro;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un comentario asociado a una idea en el foro.
 * Incluye el autor, el texto del comentario y la fecha de creación.
 */
public class Comentario implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String autor;
    private final String texto;
    private final LocalDateTime fechaCreacion;

    /**
     * Crea un nuevo comentario.
     * @param autor Nombre de usuario del autor del comentario.
     * @param texto Contenido del comentario.
     */
    public Comentario(String autor, String texto) {
        this.autor = autor;
        this.texto = texto;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Devuelve el autor del comentario.
     */
    public String getAutor() { return autor; }

    /**
     * Devuelve el texto del comentario.
     */
    public String getTexto() { return texto; }

    /**
     * Devuelve la fecha de creación en formato amigable.
     */
    public String getFechaFormateada() {
        return fechaCreacion.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
}
