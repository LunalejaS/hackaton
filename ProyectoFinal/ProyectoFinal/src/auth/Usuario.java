package auth;

import java.io.Serializable;

/**
 * Clase que representa a un usuario del sistema.
 * Implementa Serializable para permitir la persistencia de usuarios en archivos.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;

    /**
     * Constructor que inicializa un usuario con nombre de usuario y contraseña.
     * @param username Nombre de usuario.
     * @param password Contraseña del usuario.
     */
    public Usuario(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Obtiene el nombre de usuario.
     * @return Nombre de usuario.
     */
    public String getUsername() { return username; }

    /**
     * Verifica si la contraseña proporcionada coincide con la del usuario.
     * @param password Contraseña a verificar.
     * @return true si la contraseña es correcta, false en caso contrario.
     */
    public boolean checkPassword(String password) { return this.password.equals(password); }
}
