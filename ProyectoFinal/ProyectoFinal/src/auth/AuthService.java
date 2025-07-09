package auth;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

/**
 * Servicio de autenticación para gestionar usuarios.
 * Permite registrar usuarios, iniciar sesión y persistir la información en archivos.
 */
public class AuthService {
    private List<Usuario> usuarios = new ArrayList<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.dat";

    /**
     * Constructor que carga los usuarios desde archivo.
     * Si no existe el usuario admin, lo crea automáticamente.
     */
    public AuthService() {
        cargarUsuarios();
        // Si no existe el usuario admin, lo crea automáticamente
        boolean adminExiste = false;
        for (Usuario u : usuarios) {
            if (u.getUsername().equals("admin")) {
                adminExiste = true;
                break;
            }
        }
        if (!adminExiste) {
            usuarios.add(new Usuario("admin", "admin123"));
            guardarUsuarios();
        }
    }

    /**
     * Registra un nuevo usuario si el nombre de usuario no existe.
     * @param username Nombre de usuario.
     * @param password Contraseña.
     * @return true si el registro fue exitoso, false si el usuario ya existe.
     */
    public boolean registrarUsuario(String username, String password) {
        for (Usuario u : usuarios) {
            if (u.getUsername().equals(username)) {
                return false; // Usuario ya existe
            }
        }
        usuarios.add(new Usuario(username, password));
        guardarUsuarios();
        return true;
    }

    /**
     * Inicia sesión con las credenciales proporcionadas.
     * @param username Nombre de usuario.
     * @param password Contraseña.
     * @return Instancia de Usuario si las credenciales son correctas, null en caso contrario.
     */
    public Usuario iniciarSesion(String username, String password) {
        for (Usuario u : usuarios) {
            if (u.getUsername().equals(username) && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Carga la lista de usuarios desde el archivo de persistencia.
     * Si el archivo no existe o hay error, crea una lista vacía.
     */
    @SuppressWarnings("unchecked")
    private void cargarUsuarios() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARCHIVO_USUARIOS))) {
            usuarios = (List<Usuario>) ois.readObject();
        } catch (Exception e) {
            usuarios = new ArrayList<>(); // Si no existe el archivo o hay error, lista vacía
            guardarErrorEnTxt("Error cargando usuarios: " + e.getMessage());
        }
    }

    /**
     * Guarda la lista de usuarios en el archivo de persistencia.
     */
    private void guardarUsuarios() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_USUARIOS))) {
            oos.writeObject(usuarios);
        } catch (IOException e) {
            guardarErrorEnTxt("Error guardando usuarios: " + e.getMessage());
        }
    }

    /**
     * Guarda mensajes de error en un archivo de texto local para depuración.
     * @param mensaje Mensaje de error a guardar.
     */
    private void guardarErrorEnTxt(String mensaje) {
        try (FileWriter fw = new FileWriter("errores_serializacion.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(java.time.LocalDateTime.now() + " - " + mensaje);
        } catch (IOException ex) {
            // Si falla esto, no hay mucho más que hacer
        }
    }
}
