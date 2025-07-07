import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.border.Border;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

// Modelo: Clase Usuario
class Usuario implements Serializable {
    private String username;
    private String password;
    private boolean isAdmin;
    private Color colorPastel;
    private List<Mensaje> mensajesPrivados;

    public Usuario(String username, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
        this.colorPastel = generarColorPastel();
        this.mensajesPrivados = new ArrayList<>();
    }

    private Color generarColorPastel() {
        Random rand = new Random();
        return new Color(127 + rand.nextInt(129), 127 + rand.nextInt(129), 127 + rand.nextInt(129)); // 127-255
    }

    public void eliminarMensaje(Mensaje m) {
        mensajesPrivados.remove(m);
    }

    // Getters y setters
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
    public Color getColorPastel() { return colorPastel; }
    public List<Mensaje> getMensajesPrivados() { return mensajesPrivados; }
    public String getPassword() { return password; }
}

// Modelo: Clase Mensaje
class Mensaje implements Serializable {
    private String texto;
    private String autor;
    private LocalDateTime fechaPublicacion;
    private Color colorPastelAutor;
    private boolean publicadoEnMuro;
    private boolean eliminadoPorAdmin;
    private String adminEliminador;
    private LocalDateTime fechaEliminacion;

    public Mensaje(String autor, String texto, Color colorPastel, boolean publicadoEnMuro) {
        this.autor = autor;
        this.texto = texto.length() > 500 ? texto.substring(0, 500) : texto;
        this.colorPastelAutor = colorPastel;
        this.fechaPublicacion = LocalDateTime.now();
        this.publicadoEnMuro = publicadoEnMuro;
        this.eliminadoPorAdmin = false;
    }

    public void marcarComoEliminado(String admin) {
        this.eliminadoPorAdmin = true;
        this.adminEliminador = admin;
        this.fechaEliminacion = LocalDateTime.now();
    }

    // Getters
    public String getTexto() { return texto; }
    public String getAutor() { return autor; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public Color getColorPastelAutor() { return colorPastelAutor; }
    public boolean isPublicadoEnMuro() { return publicadoEnMuro; }
    public boolean isEliminadoPorAdmin() { return eliminadoPorAdmin; }
    public String getAdminEliminador() { return adminEliminador; }
    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
}

// Controlador
class ControladorPrincipal {
    private List<Usuario> usuarios;
    private List<Mensaje> muroGlobal;
    private Usuario usuarioActual;
    private VentanaPrincipal ventanaPrincipal;
    private static final String ARCHIVO_USUARIOS = "usuarios.properties";
    private static final String ARCHIVO_MURO = "muro.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ControladorPrincipal() {
        usuarios = new ArrayList<>();
        muroGlobal = new ArrayList<>();
        inicializarSuperAdmin();
        cargarDatos();
    }

    private void inicializarSuperAdmin() {
        if (usuarios.stream().noneMatch(u -> u.getUsername().equals("SuperAdmin"))) {
            Usuario superAdmin = new Usuario("SuperAdmin", "Super12345", true);
            usuarios.add(superAdmin);
            guardarUsuarios();
            guardarMensajesPrivados(superAdmin);
        }
    }

    public void iniciar() {
        new VentanaLogin(this);
    }

    public boolean registrarUsuario(String username, String password) {
        if (username.isEmpty() || password.isEmpty() || username.length() < 3 || username.length() > 20 || !username.matches("[a-zA-Z0-9]+")) {
            JOptionPane.showMessageDialog(null, "Nombre de usuario inválido (3-20 caracteres alfanuméricos) o contraseña vacía.");
            return false;
        }
        if (username.equals("SuperAdmin")) {
            JOptionPane.showMessageDialog(null, "Nombre de usuario reservado.");
            return false;
        }
        if (usuarios.stream().anyMatch(u -> u.getUsername().equals(username))) {
            JOptionPane.showMessageDialog(null, "Usuario ya registrado.");
            return false;
        }
        Usuario nuevoUsuario = new Usuario(username, password, false);
        usuarios.add(nuevoUsuario);
        guardarUsuarios();
        guardarMensajesPrivados(nuevoUsuario);
        return true;
    }

    public boolean iniciarSesion(String username, String password) {
        Optional<Usuario> usuario = usuarios.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst();
        if (usuario.isPresent()) {
            usuarioActual = usuario.get();
            cargarMensajesPrivados();
            ventanaPrincipal = new VentanaPrincipal(this, usuarioActual);
            return true;
        }
        JOptionPane.showMessageDialog(null, "Credenciales incorrectas.");
        return false;
    }

    public void publicarMensaje(String texto, boolean enMuro) {
        if (texto.length() > 500) {
            JOptionPane.showMessageDialog(null, "¡Has excedido el límite de 500 caracteres!");
            return;
        }
        Mensaje mensaje = new Mensaje(usuarioActual.getUsername(), texto, usuarioActual.getColorPastel(), enMuro);
        if (enMuro) {
            muroGlobal.add(mensaje);
            guardarMuro();
            ventanaPrincipal.actualizarMuro();
        } else {
            usuarioActual.getMensajesPrivados().add(mensaje);
            guardarMensajesPrivados();
            ventanaPrincipal.actualizarArchivoPersonal();
        }
    }

    public void eliminarMensajeMuro(Mensaje mensaje, boolean esAdmin, boolean esSuperAdmin) {
        if (esSuperAdmin) {
            mensaje.marcarComoEliminado(usuarioActual.getUsername());
        } else if (esAdmin && !mensaje.getAutor().equals("SuperAdmin")) {
            mensaje.marcarComoEliminado(usuarioActual.getUsername());
        } else if (mensaje.getAutor().equals(usuarioActual.getUsername())) {
            muroGlobal.remove(mensaje);
        } else {
            JOptionPane.showMessageDialog(null, "No tienes permiso para eliminar este mensaje.");
            return;
        }
        guardarMuro();
        ventanaPrincipal.actualizarMuro();
    }

    public void eliminarMensajePrivado(Mensaje mensaje) {
        usuarioActual.eliminarMensaje(mensaje);
        guardarMensajesPrivados();
        ventanaPrincipal.actualizarArchivoPersonal();
    }

    public void cerrarSesion() {
        guardarDatos();
        usuarioActual = null;
        ventanaPrincipal.dispose();
        new VentanaLogin(this);
    }

    public void actualizarAdminStatus(String username, boolean isAdmin) {
        usuarios.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .ifPresent(u -> u.setAdmin(isAdmin));
        guardarUsuarios();
    }

    private void cargarDatos() {
        cargarUsuarios();
        cargarMuro();
    }

    private void cargarUsuarios() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 4) {
                    String username = partes[0];
                    String password = partes[1];
                    boolean isAdmin = Boolean.parseBoolean(partes[2]);
                    String[] rgb = partes[3].split(":");
                    Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                    Usuario usuario = new Usuario(username, password, isAdmin);
                    usuarios.add(usuario);
                }
            }
        } catch (IOException e) {
            try {
                new File(ARCHIVO_USUARIOS).createNewFile();
                inicializarSuperAdmin();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error al crear archivo de usuarios.");
            }
        }
    }

    private void cargarMuro() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_MURO))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split("\\|");
                if (partes.length >= 5) {
                    String autor = partes[0];
                    LocalDateTime fecha = LocalDateTime.parse(partes[1], FORMATTER);
                    String texto = partes[2];
                    String[] rgb = partes[3].split(":");
                    Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                    boolean publicado = Boolean.parseBoolean(partes[4]);
                    Mensaje mensaje = new Mensaje(autor, texto, color, publicado);
                    if (partes.length == 7) {
                        mensaje.marcarComoEliminado(partes[5]);
                    }
                    muroGlobal.add(mensaje);
                }
            }
        } catch (IOException e) {
            try {
                new File(ARCHIVO_MURO).createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error al crear archivo de muro.");
            }
        }
    }

    private void cargarMensajesPrivados() {
        String archivo = "archivo_" + usuarioActual.getUsername() + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split("\\|");
                if (partes.length == 5) {
                    String autor = partes[0];
                    LocalDateTime fecha = LocalDateTime.parse(partes[1], FORMATTER);
                    String texto = partes[2];
                    String[] rgb = partes[3].split(":");
                    Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                    boolean publicado = Boolean.parseBoolean(partes[4]);
                    usuarioActual.getMensajesPrivados().add(new Mensaje(autor, texto, color, publicado));
                }
            }
        } catch (IOException e) {
            try {
                new File(archivo).createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error al crear archivo personal.");
            }
        }
    }

    private void guardarUsuarios() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS))) {
            for (Usuario u : usuarios) {
                Color c = u.getColorPastel();
                writer.write(String.format("%s,%s,%b,%d:%d:%d\n", u.getUsername(), u.getPassword(), u.isAdmin(), c.getRed(), c.getGreen(), c.getBlue()));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar usuarios.");
        }
    }

    private void guardarMuro() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MURO))) {
            for (Mensaje m : muroGlobal) {
                Color c = m.getColorPastelAutor();
                String linea = String.format("%s|%s|%s|%d:%d:%d|%b", m.getAutor(), m.getFechaPublicacion().format(FORMATTER), m.getTexto(), c.getRed(), c.getGreen(), c.getBlue(), m.isPublicadoEnMuro());
                if (m.isEliminadoPorAdmin()) {
                    linea += String.format("|%s|%s", m.getAdminEliminador(), m.getFechaEliminacion().format(FORMATTER));
                }
                writer.write(linea + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar muro.");
        }
    }

    private void guardarMensajesPrivados() {
        guardarMensajesPrivados(usuarioActual);
    }

    private void guardarMensajesPrivados(Usuario usuario) {
        String archivo = "archivo_" + usuario.getUsername() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            for (Mensaje m : usuario.getMensajesPrivados()) {
                Color c = m.getColorPastelAutor();
                writer.write(String.format("%s|%s|%s|%d:%d:%d|%b\n", m.getAutor(), m.getFechaPublicacion().format(FORMATTER), m.getTexto(), c.getRed(), c.getGreen(), c.getBlue(), m.isPublicadoEnMuro()));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar mensajes privados.");
        }
    }

    private void guardarDatos() {
        guardarUsuarios();
        guardarMuro();
        guardarMensajesPrivados();
    }

    public List<Mensaje> getMuroGlobal() { return muroGlobal; }
    public Usuario getUsuarioActual() { return usuarioActual; }
    public List<Usuario> getUsuarios() { return usuarios; }
    public boolean isSuperAdmin(String username) { return username.equals("SuperAdmin"); }
}

// Vista: Ventana de Login
class VentanaLogin extends JFrame {
    private ControladorPrincipal controlador;

    public VentanaLogin(ControladorPrincipal controlador) {
        this.controlador = controlador;
        setTitle("Meme Forum - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel lblUsername = new JLabel("Usuario:");
        JTextField txtUsername = new JTextField(15);
        JLabel lblPassword = new JLabel("Contraseña:");
        JPasswordField txtPassword = new JPasswordField(15);
        JButton btnRegistrar = new JButton("Registrarse");
        JButton btnIniciar = new JButton("Iniciar Sesión");
        JButton btnSalir = new JButton("Salir");

        gbc.gridx = 0; gbc.gridy = 0; add(lblUsername, gbc);
        gbc.gridx = 1; add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(lblPassword, gbc);
        gbc.gridx = 1; add(txtPassword, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; add(btnRegistrar, gbc);
        gbc.gridy = 3; add(btnIniciar, gbc);
        gbc.gridy = 4; add(btnSalir, gbc);

        btnRegistrar.addActionListener(e -> {
            if (controlador.registrarUsuario(txtUsername.getText(), new String(txtPassword.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Usuario registrado con éxito. Contacta al SuperAdmin para obtener permisos de administrador.");
            }
        });

        btnIniciar.addActionListener(e -> {
            if (controlador.iniciarSesion(txtUsername.getText(), new String(txtPassword.getPassword()))) {
                dispose();
            }
        });

        btnSalir.addActionListener(e -> System.exit(0));

        setVisible(true);
    }
}

// Vista: Ventana de Gestión de Admins
class VentanaAdmin extends JFrame {
    private ControladorPrincipal controlador;

    public VentanaAdmin(ControladorPrincipal controlador) {
        this.controlador = controlador;
        setTitle("Gestión de Administradores");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel panelUsuarios = new JPanel();
        panelUsuarios.setLayout(new BoxLayout(panelUsuarios, BoxLayout.Y_AXIS));
        JScrollPane scrollUsuarios = new JScrollPane(panelUsuarios);
        add(scrollUsuarios, BorderLayout.CENTER);

        // Lista de usuarios con checkboxes
        for (Usuario u : controlador.getUsuarios()) {
            if (!u.getUsername().equals("SuperAdmin")) {
                JPanel panelUsuario = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JCheckBox chkAdmin = new JCheckBox(u.getUsername(), u.isAdmin());
                chkAdmin.addActionListener(e -> controlador.actualizarAdminStatus(u.getUsername(), chkAdmin.isSelected()));
                panelUsuario.add(chkAdmin);
                panelUsuarios.add(panelUsuario);
            }
        }

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        add(btnCerrar, BorderLayout.SOUTH);

        setVisible(true);
    }
}

// Vista: Ventana Principal
class VentanaPrincipal extends JFrame {
    private ControladorPrincipal controlador;
    private Usuario usuarioActual;
    private JPanel panelMuro;
    private JTextArea txtMensaje;
    private JPanel panelArchivo;

    public VentanaPrincipal(ControladorPrincipal controlador, Usuario usuarioActual) {
        this.controlador = controlador;
        this.usuarioActual = usuarioActual;
        setTitle("Meme Forum - " + usuarioActual.getUsername());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Muro Global (Izquierda)
        panelMuro = new JPanel();
        panelMuro.setLayout(new BoxLayout(panelMuro, BoxLayout.Y_AXIS));
        JScrollPane scrollMuro = new JScrollPane(panelMuro);
        scrollMuro.setPreferredSize(new Dimension(320, 600)); // 40% del ancho
        add(scrollMuro, BorderLayout.WEST);

        // Panel Derecho
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setPreferredSize(new Dimension(480, 600)); // 60% del ancho

        // Panel Creación (Superior Derecho)
        JPanel panelCreacion = new JPanel(new BorderLayout());
        txtMensaje = new JTextArea(5, 30);
        txtMensaje.setToolTipText("Máximo 500 caracteres");
        txtMensaje.setLineWrap(true);
        txtMensaje.setWrapStyleWord(true);
        JScrollPane scrollMensaje = new JScrollPane(txtMensaje);
        JPanel panelBotones = new JPanel();
        JButton btnPublicar = new JButton("Publicar en muro");
        btnPublicar.setToolTipText("Pulsa para publicar en el muro");
        JButton btnGuardar = new JButton("Guardar en archivo personal");
        btnGuardar.setToolTipText("Pulsa para guardar privadamente");
        panelBotones.add(btnPublicar);
        panelBotones.add(btnGuardar);
        panelCreacion.add(scrollMensaje, BorderLayout.CENTER);
        panelCreacion.add(panelBotones, BorderLayout.SOUTH);
        panelDerecho.add(panelCreacion, BorderLayout.NORTH);

        // Archivo Personal (Inferior Derecho)
        panelArchivo = new JPanel();
        panelArchivo.setLayout(new BoxLayout(panelArchivo, BoxLayout.Y_AXIS));
        JScrollPane scrollArchivo = new JScrollPane(panelArchivo);
        panelDerecho.add(scrollArchivo, BorderLayout.CENTER);

        add(panelDerecho, BorderLayout.CENTER);

        // Menú
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Opciones");
        JMenuItem itemCerrar = new JMenuItem("Cerrar Sesión");
        menu.add(itemCerrar);
        if (controlador.isSuperAdmin(usuarioActual.getUsername())) {
            JMenuItem itemAdmin = new JMenuItem("Gestionar Administradores");
            itemAdmin.addActionListener(e -> new VentanaAdmin(controlador));
            menu.add(itemAdmin);
        }
        menuBar.add(menu);
        setJMenuBar(menuBar);

        // Listeners
        btnPublicar.addActionListener(e -> controlador.publicarMensaje(txtMensaje.getText(), true));
        btnGuardar.addActionListener(e -> controlador.publicarMensaje(txtMensaje.getText(), false));
        itemCerrar.addActionListener(e -> controlador.cerrarSesion());

        actualizarMuro();
        actualizarArchivoPersonal();
        setVisible(true);
    }

    public void actualizarMuro() {
        panelMuro.removeAll();
        for (Mensaje m : controlador.getMuroGlobal()) {
            JPanel panelMensaje = new JPanel(new BorderLayout(10, 10));
            boolean esAdmin = controlador.getUsuarios().stream()
                    .filter(u -> u.getUsername().equals(m.getAutor()))
                    .findFirst()
                    .map(u -> u.isAdmin())
                    .orElse(false);
            boolean esSuperAdmin = m.getAutor().equals("SuperAdmin");
            panelMensaje.setBackground(esAdmin ? Color.BLACK : Color.WHITE);
            panelMensaje.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            panelMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

            // Icono placeholder
            JPanel iconPanel = new JPanel();
            iconPanel.setBackground(esAdmin ? Color.DARK_GRAY : Color.GRAY);
            iconPanel.setPreferredSize(new Dimension(40, 40));
            panelMensaje.add(iconPanel, BorderLayout.WEST);

            // Texto del mensaje
            String texto = m.isEliminadoPorAdmin() ?
                    String.format("Este mensaje fue eliminado por Admin %s el %s",
                            m.getAdminEliminador(), m.getFechaEliminacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))) :
                    String.format("%s (%s):\n%s", m.getAutor(), m.getFechaPublicacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), m.getTexto());
            JTextArea lblMensaje = new JTextArea(texto);
            lblMensaje.setLineWrap(true);
            lblMensaje.setWrapStyleWord(true);
            lblMensaje.setEditable(false);
            lblMensaje.setBackground(esAdmin ? Color.BLACK : Color.WHITE);
            lblMensaje.setForeground(esSuperAdmin ? Color.BLACK : (esAdmin ? m.getColorPastelAutor() : Color.BLACK));
            lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 14));
            panelMensaje.add(lblMensaje, BorderLayout.CENTER);

            JPopupMenu popup = new JPopupMenu();
            JMenuItem itemEliminar = new JMenuItem("Eliminar mensaje");
            popup.add(itemEliminar);
            panelMensaje.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e) &&
                            (controlador.isSuperAdmin(usuarioActual.getUsername()) ||
                             (usuarioActual.isAdmin() && !m.getAutor().equals("SuperAdmin")) ||
                             m.getAutor().equals(usuarioActual.getUsername()))) {
                        popup.show(panelMensaje, e.getX(), e.getY());
                    }
                }
            });
            itemEliminar.addActionListener(e -> controlador.eliminarMensajeMuro(m, usuarioActual.isAdmin(), controlador.isSuperAdmin(usuarioActual.getUsername())));

            panelMuro.add(panelMensaje);
            panelMuro.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        panelMuro.revalidate();
        panelMuro.repaint();
    }

    public void actualizarArchivoPersonal() {
        panelArchivo.removeAll();
        for (Mensaje m : usuarioActual.getMensajesPrivados()) {
            JPanel panelMensaje = new JPanel(new BorderLayout(10, 10));
            boolean esAdmin = usuarioActual.isAdmin();
            boolean esSuperAdmin = usuarioActual.getUsername().equals("SuperAdmin");
            panelMensaje.setBackground(esAdmin ? Color.BLACK : Color.WHITE);
            panelMensaje.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            panelMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

            // Icono placeholder
            JPanel iconPanel = new JPanel();
            iconPanel.setBackground(esAdmin ? Color.DARK_GRAY : Color.GRAY);
            iconPanel.setPreferredSize(new Dimension(40, 40));
            panelMensaje.add(iconPanel, BorderLayout.WEST);

            // Texto del mensaje
            JTextArea lblMensaje = new JTextArea(String.format("%s (%s):\n%s",
                    m.getAutor(), m.getFechaPublicacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), m.getTexto()));
            lblMensaje.setLineWrap(true);
            lblMensaje.setWrapStyleWord(true);
            lblMensaje.setEditable(false);
            lblMensaje.setBackground(esAdmin ? Color.BLACK : Color.WHITE);
            lblMensaje.setForeground(esSuperAdmin ? Color.BLACK : (esAdmin ? m.getColorPastelAutor() : Color.BLACK));
            lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 14));
            panelMensaje.add(lblMensaje, BorderLayout.CENTER);

            JPopupMenu popup = new JPopupMenu();
            JMenuItem itemEliminar = new JMenuItem("Eliminar mensaje");
            popup.add(itemEliminar);
            panelMensaje.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        popup.show(panelMensaje, e.getX(), e.getY());
                    }
                }
            });
            itemEliminar.addActionListener(e -> controlador.eliminarMensajePrivado(m));

            panelArchivo.add(panelMensaje);
            panelArchivo.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        panelArchivo.revalidate();
        panelArchivo.repaint();
    }
}

// Clase principal
public class MemeForumApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControladorPrincipal().iniciar());
    }
}