package auth;

/**
 * Clase que representa al usuario administrador del sistema.
 * Hereda de Usuario y define credenciales predeterminadas ("admin", "admin123").
 * Se utiliza para tareas administrativas como la gestión de ideas y usuarios.
 */
public class Administrador extends Usuario {
    /**
     * Constructor que inicializa el usuario administrador con credenciales predeterminadas.
     */
    public Administrador() {
        super("admin", "admin123"); // Usuario y contraseña predeterminados
    }
}
