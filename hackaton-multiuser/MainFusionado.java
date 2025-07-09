import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

// Clase que representa al usuario administrador del sistema
class Administrador extends Usuario {
    public Administrador() {
        super("admin", "admin123");
    }
}

// Servicio de autenticación para gestionar usuarios
class AuthService {
    private List<Usuario> usuarios = new ArrayList<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.dat";

    public AuthService() {
        cargarUsuarios();
        boolean adminExiste = usuarios.stream().anyMatch(u -> u.getUsername().equals("admin"));
        if (!adminExiste) {
            usuarios.add(new Administrador());
            guardarUsuarios();
        }
    }

    public boolean registrarUsuario(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) return false;
        if (usuarios.stream().anyMatch(u -> u.getUsername().equals(username))) return false;
        usuarios.add(new Usuario(username, password));
        guardarUsuarios();
        return true;
    }

    public Usuario iniciarSesion(String username, String password) {
        return usuarios.stream()
                .filter(u -> u.getUsername().equals(username) && u.checkPassword(password))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private void cargarUsuarios() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARCHIVO_USUARIOS))) {
            usuarios = (List<Usuario>) ois.readObject();
        } catch (Exception e) {
            usuarios = new ArrayList<>();
            guardarErrorEnTxt("Error cargando usuarios: " + e.getMessage());
        }
    }

    private void guardarUsuarios() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_USUARIOS))) {
            oos.writeObject(usuarios);
        } catch (IOException e) {
            guardarErrorEnTxt("Error guardando usuarios: " + e.getMessage());
        }
    }

    private void guardarErrorEnTxt(String mensaje) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("errores_serializacion.txt", true)))) {
            out.println(LocalDateTime.now() + " - " + mensaje);
        } catch (IOException ignored) {}
    }
}

// Clase que representa a un usuario del sistema
class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;

    public Usuario(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public boolean checkPassword(String password) { return this.password.equals(password); }
}

// Representa un comentario asociado a una idea
class Comentario implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String autor;
    private final String texto;
    private final LocalDateTime fechaCreacion;

    public Comentario(String autor, String texto) {
        this.autor = autor;
        this.texto = texto;
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getAutor() { return autor; }
    public String getTexto() { return texto; }
    public String getFechaFormateada() {
        return fechaCreacion.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
}

// Clase para la gestión y visualización del foro
class Foro {
    private final IdeaService ideaService;

    public Foro(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

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
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JOptionPane.showMessageDialog(null, scrollPane, "Muro Global de Ideas", JOptionPane.PLAIN_MESSAGE);
    }

    public void mostrarIdeasPendientes() {
        List<Idea> pendientes = ideaService.getIdeasPendientes();
        if (pendientes.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "<html><div style='font-size:16px;'>No hay ideas pendientes para revisar.</div></html>",
                    "Ideas Pendientes", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new String[]{"Nombre", "Código", "Idea", "Aprobar", "Desaprobar", "Eliminar"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column >= 3; }
        };

        for (Idea idea : pendientes) {
            model.addRow(new Object[]{idea.getNombreEstudiante(), idea.getCodigoEstudiante(), idea.getContenido(), "Aprobar", "Desaprobar", "Eliminar"});
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row < 0 || col < 3) return;

                List<Idea> pendientesActuales = ideaService.getIdeasPendientes();
                if (row >= pendientesActuales.size()) return;

                Idea idea = pendientesActuales.get(row);
                if (col == 3) {
                    ideaService.aprobarIdea(idea);
                    JOptionPane.showMessageDialog(null, "Idea aprobada.");
                } else if (col == 4) {
                    ideaService.desaprobarIdea(idea);
                    JOptionPane.showMessageDialog(null, "Idea desaprobada.");
                } else if (col == 5) {
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "¿Estás seguro de que deseas eliminar esta idea permanentemente?",
                            "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
        scrollPane.setPreferredSize(new Dimension(800, 300));
        JOptionPane.showMessageDialog(null, scrollPane, "Ideas Pendientes", JOptionPane.PLAIN_MESSAGE);
    }

    public void agregarIdea(String nombreEstudiante, String codigoEstudiante, String titulo, String contenidoIdea, String imageUrl) {
        Idea nuevaIdea = new Idea(nombreEstudiante.trim(), codigoEstudiante.trim(), titulo.trim(), contenidoIdea.trim(), imageUrl.trim());
        ideaService.agregarIdea(nuevaIdea);
        JOptionPane.showMessageDialog(null, "Idea enviada para aprobación del administrador.");
    }
}

// Representa una idea publicada en el foro
class Idea implements Serializable {
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
    private transient ImageIcon loadedImage;
    private transient boolean isLoading = false;

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

    public List<Comentario> getComentarios() {
        if (comentarios == null) comentarios = new ArrayList<>();
        return comentarios;
    }

    public void agregarComentario(Comentario comentario) { getComentarios().add(comentario); }
    public void eliminarComentario(Comentario comentario) { getComentarios().remove(comentario); }
    public String getNombreEstudiante() { return nombreEstudiante; }
    public String getCodigoEstudiante() { return codigoEstudiante; }
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getImageUrl() { return imageUrl; }
    public Estado getEstado() { return estado; }
    public void aprobar() { this.estado = Estado.APROBADA; }
    public void desaprobar() { this.estado = Estado.DESAPROBADA; }
    public String getDescripcion() { return contenido; }

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
        double sum = getVotesMap().values().stream().mapToInt(Integer::intValue).sum();
        return sum / getVotesMap().size();
    }

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
                try {
                    loadedImage = get();
                } catch (Exception ignored) {}
                finally {
                    isLoading = false;
                    componentToRepaint.repaint();
                }
            }
        }.execute();
        return null;
    }
}

// Renderizador personalizado para listas de ideas
class IdeaListCellRenderer implements ListCellRenderer<Idea> {
    private final Usuario usuarioActual;
    private final IdeaService ideaService;

    public IdeaListCellRenderer(Usuario usuarioActual, IdeaService ideaService) {
        this.usuarioActual = usuarioActual;
        this.ideaService = ideaService;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Idea> list, Idea idea, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        IdeaPanel panel = new IdeaPanel(idea, usuarioActual, ideaService);
        panel.setBackground(isSelected ? new Color(220, 235, 255) : Color.WHITE);
        return panel;
    }
}

// Panel visual para mostrar una idea
class IdeaPanel extends JPanel {
    private final Idea idea;
    private final IdeaService ideaService;

    public IdeaPanel(Idea idea, Usuario usuarioActual, IdeaService ideaService) {
        this.idea = idea;
        this.ideaService = ideaService;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 10, 0),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                        new EmptyBorder(20, 20, 20, 20)
                )
        ));
        setBackground(Color.WHITE);

        JPanel panelIzquierdo = new JPanel(new BorderLayout(0, 20));
        panelIzquierdo.setOpaque(false);

        JPanel topTextPanel = new JPanel();
        topTextPanel.setLayout(new BoxLayout(topTextPanel, BoxLayout.Y_AXIS));
        topTextPanel.setOpaque(false);

        JLabel tituloLabel = new JLabel(idea.getTitulo());
        tituloLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tituloLabel.setForeground(new Color(150, 0, 0));
        topTextPanel.add(tituloLabel);
        topTextPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel autorLabel = new JLabel("Por: " + idea.getNombreEstudiante() + " (" + idea.getCodigoEstudiante() + ")");
        autorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        autorLabel.setForeground(new Color(150, 150, 150));
        topTextPanel.add(autorLabel);
        topTextPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JTextArea contenidoArea = new JTextArea(idea.getContenido());
        contenidoArea.setEditable(false);
        contenidoArea.setLineWrap(true);
        contenidoArea.setWrapStyleWord(true);
        contenidoArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contenidoArea.setOpaque(false);
        topTextPanel.add(contenidoArea);

        panelIzquierdo.add(topTextPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setOpaque(false);

        if (usuarioActual != null && !usuarioActual.getUsername().equals("admin")) {
            bottomPanel.add(new StarRatingPanel(idea, usuarioActual, ideaService));
        } else {
            String avgText = String.format("Calificación: %.1f ★ (%d votos)", idea.getAverageRating(), idea.getVoteCount());
            JLabel avgLabel = new JLabel(avgText);
            avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            bottomPanel.add(avgLabel);
        }

        bottomPanel.add(Box.createHorizontalGlue());

        JButton verDetallesButton = new JButton("Comentarios");
        verDetallesButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        verDetallesButton.setBackground(new Color(204, 0, 0));
        verDetallesButton.setForeground(Color.WHITE);
        verDetallesButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        verDetallesButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new VentanaDetalleIdea(owner, idea, ideaService, usuarioActual).setVisible(true);
        });
        bottomPanel.add(verDetallesButton);
        bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        if (usuarioActual != null && usuarioActual.getUsername().equals("admin")) {
            JButton deleteButton = new JButton("Eliminar Idea");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            deleteButton.addActionListener(e -> eliminarIdea());
            bottomPanel.add(deleteButton);
        }

        panelIzquierdo.add(bottomPanel, BorderLayout.SOUTH);
        add(panelIzquierdo, BorderLayout.CENTER);

        if (idea.getImageUrl() != null && !idea.getImageUrl().isEmpty()) {
            JLabel imageLabel = new JLabel("Cargando imagen...", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 200));
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(imageLabel, BorderLayout.EAST);
            ImageIcon icon = idea.getImageIcon(this);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imageLabel.setText(null);
            }
        }
    }

    private void eliminarIdea() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que deseas eliminar esta idea permanentemente?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            ideaService.eliminarIdea(idea);
            ideaService.guardarIdeas(); // Save changes after deletion
            Container parentContainer = getParent();
            if (parentContainer != null) {
                parentContainer.remove(this);
                parentContainer.revalidate();
                parentContainer.repaint();
            }
        }
    }
}

// Servicio para gestionar la persistencia y operaciones sobre las ideas
class IdeaService {
    private List<Idea> ideas = new ArrayList<>();
    private static final String ARCHIVO_IDEAS = "ideas.dat";

    public IdeaService() { cargarIdeas(); }

    public List<Idea> getIdeas() { return new ArrayList<>(ideas); }
    public List<Idea> getIdeasPendientes() {
        return ideas.stream().filter(idea -> idea.getEstado() == Idea.Estado.PENDIENTE).toList();
    }
    public List<Idea> getIdeasAprobadas() {
        return ideas.stream().filter(idea -> idea.getEstado() == Idea.Estado.APROBADA).toList();
    }
    public void agregarIdea(Idea idea) {
        ideas.add(idea);
        guardarIdeas();
    }
    public void aprobarIdea(Idea idea) {
        idea.aprobar();
        guardarIdeas();
    }
    public void desaprobarIdea(Idea idea) {
        idea.desaprobar();
        guardarIdeas();
    }
    public void eliminarIdea(Idea idea) {
        ideas.remove(idea);
        guardarIdeas();
    }

    public void guardarIdeas() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_IDEAS))) {
            oos.writeObject(ideas);
        } catch (IOException e) {
            guardarErrorEnTxt("Error guardando ideas: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void cargarIdeas() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARCHIVO_IDEAS))) {
            ideas = (List<Idea>) ois.readObject();
        } catch (Exception e) {
            ideas = new ArrayList<>();
            guardarErrorEnTxt("Error cargando ideas: " + e.getMessage());
        }
    }

    private void guardarErrorEnTxt(String mensaje) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("errores_serializacion.txt", true)))) {
            out.println(LocalDateTime.now() + " - " + mensaje);
        } catch (IOException ignored) {}
    }
}

// Panel de calificación con estrellas
class StarRatingPanel extends JPanel {
    private final JLabel[] stars = new JLabel[5];
    private final Idea idea;
    private final Usuario usuario;
    private final IdeaService ideaService;
    private final Color starColor = new Color(255, 204, 0);

    public StarRatingPanel(Idea idea, Usuario usuario, IdeaService ideaService) {
        this.idea = idea;
        this.usuario = usuario;
        this.ideaService = ideaService;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        for (int i = 0; i < 5; i++) {
            stars[i] = new JLabel("☆");
            stars[i].setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
            stars[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            stars[i].setForeground(Color.LIGHT_GRAY);
            add(stars[i]);
            if (i < 4) add(Box.createRigidArea(new Dimension(5, 0)));

            final int rating = i + 1;
            stars[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    idea.addVote(usuario.getUsername(), rating);
                    ideaService.guardarIdeas();
                    updateStars(rating);
                }

                @Override
                public void mouseEntered(MouseEvent e) { updateStars(rating, true); }
                @Override
                public void mouseExited(MouseEvent e) { updateStars(idea.getUserVote(usuario.getUsername())); }
            });
        }
        updateStars(idea.getUserVote(usuario.getUsername()));
    }

    private void updateStars(int rating, boolean isPreview) {
        for (int i = 0; i < 5; i++) {
            stars[i].setText(i < rating ? "★" : "☆");
            stars[i].setForeground(i < rating ? starColor : Color.LIGHT_GRAY);
        }
    }

    private void updateStars(int rating) { updateStars(rating, false); }
}

// Ventana de detalle para una idea
class VentanaDetalleIdea extends JDialog {
    private final Idea idea;
    private final Usuario usuarioActual;
    private final IdeaService ideaService;
    private final JPanel listaComentariosPanel;

    public VentanaDetalleIdea(Window owner, Idea idea, IdeaService ideaService, Usuario usuarioActual) {
        super(owner, "Detalle de la Idea", ModalityType.APPLICATION_MODAL);
        this.idea = idea;
        this.ideaService = ideaService;
        this.usuarioActual = usuarioActual;
        setSize(750, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 20));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel panelIdea = new JPanel(new BorderLayout(10, 10));
        String textoCompleto = String.format(
                "<html><h1 style='color: rgb(150,0,0); margin-bottom: 2px;'>%s</h1>" +
                        "<p style='color: gray; font-style: italic; margin-top: 0px;'>Por: %s (%s)</p><br><p>%s</p></html>",
                idea.getTitulo(), idea.getNombreEstudiante(), idea.getCodigoEstudiante(), idea.getContenido().replace("\n", "<br>"));
        JEditorPane editorPane = new JEditorPane("text/html", textoCompleto);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        panelIdea.add(editorPane, BorderLayout.CENTER);

        if (idea.getImageUrl() != null && !idea.getImageUrl().isEmpty()) {
            JLabel imageLabel = new JLabel("Cargando...", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 200));
            panelIdea.add(imageLabel, BorderLayout.EAST);
            ImageIcon icon = idea.getImageIcon(imageLabel);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imageLabel.setText(null);
            }
        }

        contentPanel.add(panelIdea, BorderLayout.NORTH);

        JPanel seccionComentarios = new JPanel(new BorderLayout(10, 10));
        seccionComentarios.setBorder(BorderFactory.createTitledBorder("Comentarios"));
        listaComentariosPanel = new JPanel();
        listaComentariosPanel.setLayout(new BoxLayout(listaComentariosPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollComentarios = new JScrollPane(listaComentariosPanel);
        scrollComentarios.setBorder(null);
        seccionComentarios.add(scrollComentarios, BorderLayout.CENTER);

        JPanel panelNuevoComentario = new JPanel(new BorderLayout(5, 5));
        JTextField campoComentario = new JTextField();
        JButton botonComentar = new JButton("Comentar");
        botonComentar.addActionListener(e -> {
            String texto = campoComentario.getText().trim();
            if (!texto.isEmpty()) {
                String autor = (usuarioActual != null) ? usuarioActual.getUsername() : "Anónimo";
                idea.agregarComentario(new Comentario(autor, texto));
                ideaService.guardarIdeas();
                campoComentario.setText("");
                reconstruirListaComentarios();
            }
        });
        panelNuevoComentario.add(campoComentario, BorderLayout.CENTER);
        panelNuevoComentario.add(botonComentar, BorderLayout.EAST);
        seccionComentarios.add(panelNuevoComentario, BorderLayout.SOUTH);

        add(new JScrollPane(contentPanel));
        reconstruirListaComentarios();
    }

    private void reconstruirListaComentarios() {
        listaComentariosPanel.removeAll();
        for (Comentario comentario : idea.getComentarios()) {
            JPanel panelComentario = new JPanel(new BorderLayout(10, 2));
            panelComentario.setBorder(new EmptyBorder(5, 5, 5, 5));
            JLabel autorLabel = new JLabel(String.format(
                    "<html><b>%s</b> <font color='gray'>(%s)</font></html>",
                    comentario.getAutor(), comentario.getFechaFormateada()));
            panelComentario.add(autorLabel, BorderLayout.NORTH);
            JTextArea textoArea = new JTextArea(comentario.getTexto());
            textoArea.setLineWrap(true);
            textoArea.setWrapStyleWord(true);
            textoArea.setEditable(false);
            panelComentario.add(textoArea, BorderLayout.CENTER);

            if (usuarioActual != null && usuarioActual.getUsername().equals("admin")) {
                JButton botonEliminar = new JButton("X");
                botonEliminar.setForeground(Color.RED);
                botonEliminar.setMargin(new Insets(0, 4, 0, 4));
                botonEliminar.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "¿Eliminar este comentario?", "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        idea.eliminarComentario(comentario);
                        ideaService.guardarIdeas();
                        reconstruirListaComentarios();
                    }
                });
                panelComentario.add(botonEliminar, BorderLayout.EAST);
            }
            listaComentariosPanel.add(panelComentario);
        }
        listaComentariosPanel.revalidate();
        listaComentariosPanel.repaint();
    }
}

// Clase utilitaria para diálogos de ingreso de credenciales
class DialogUtils {
    public static String[] pedirUsuarioYContrasena(String titulo) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel usuarioLabel = new JLabel("Usuario:");
        usuarioLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JTextField usuarioField = new JTextField();
        usuarioField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usuarioField.setPreferredSize(new Dimension(200, 30));

        JLabel contrasenaLabel = new JLabel("Contraseña:");
        contrasenaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JPasswordField contrasenaField = new JPasswordField();
        contrasenaField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contrasenaField.setPreferredSize(new Dimension(200, 30));

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(usuarioLabel, gbc);
        gbc.gridx = 1;
        panel.add(usuarioField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(contrasenaLabel, gbc);
        gbc.gridx = 1;
        panel.add(contrasenaField, gbc);
        panel.setPreferredSize(new Dimension(420, 130));

        int result = JOptionPane.showConfirmDialog(null, panel, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String usuario = usuarioField.getText();
            String contrasena = new String(contrasenaField.getPassword());
            return new String[]{usuario, contrasena};
        }
        return null;
    }
}

// Diálogo para ingresar nuevas ideas
class IdeaInputDialog {
    private static Timer urlTypingTimer;

    public static String[] mostrarDialogo() {
        JTextField nombreField = new JTextField();
        JTextField codigoField = new JTextField();
        JTextField tituloField = new JTextField();
        JTextArea contenidoArea = new JTextArea(5, 20);
        JTextField imageUrlField = new JTextField();
        JLabel previewLabel = new JLabel("Vista Previa", SwingConstants.CENTER);

        contenidoArea.setLineWrap(true);
        contenidoArea.setWrapStyleWord(true);
        previewLabel.setPreferredSize(new Dimension(200, 150));
        previewLabel.setBorder(BorderFactory.createEtchedBorder());
        previewLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Color.WHITE);

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 16);
        nombreField.setFont(mainFont);
        codigoField.setFont(mainFont);
        tituloField.setFont(mainFont);
        contenidoArea.setFont(mainFont);
        imageUrlField.setFont(mainFont);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel textDataPanel = new JPanel();
        textDataPanel.setLayout(new BoxLayout(textDataPanel, BoxLayout.Y_AXIS));
        textDataPanel.add(new JLabel("Nombre completo:"));
        textDataPanel.add(nombreField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Código estudiantil:"));
        textDataPanel.add(codigoField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Título de la idea:"));
        textDataPanel.add(tituloField);
        textDataPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textDataPanel.add(new JLabel("Contenido de la idea:"));
        JScrollPane scrollPane = new JScrollPane(contenidoArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        textDataPanel.add(scrollPane);
        panel.add(textDataPanel, BorderLayout.CENTER);

        JPanel imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setPreferredSize(new Dimension(220, 250));
        imagePanel.add(new JLabel("URL de la imagen (opcional):"), BorderLayout.NORTH);
        imagePanel.add(imageUrlField, BorderLayout.CENTER);
        imagePanel.add(previewLabel, BorderLayout.SOUTH);
        panel.add(imagePanel, BorderLayout.EAST);

        urlTypingTimer = new Timer(500, e -> updateImagePreview(imageUrlField, previewLabel));
        urlTypingTimer.setRepeats(false);
        imageUrlField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { urlTypingTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { urlTypingTimer.restart(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Enviar Idea",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            urlTypingTimer.stop();
            if (result != JOptionPane.OK_OPTION) return null;

            String nombre = nombreField.getText().trim();
            String codigo = codigoField.getText().trim();
            String titulo = tituloField.getText().trim();
            String contenido = contenidoArea.getText().trim();
            String imageUrl = imageUrlField.getText().trim();

            ArrayList<String> errores = new ArrayList<>();
            if (nombre.isEmpty()) errores.add("• Nombre completo");
            if (!codigo.matches("\\d{11}")) errores.add("• Código estudiantil (11 dígitos numéricos)");
            if (titulo.isEmpty()) errores.add("• Título de la idea");
            if (contenido.isEmpty()) errores.add("• Contenido de la idea");

            if (errores.isEmpty()) return new String[]{nombre, codigo, titulo, contenido, imageUrl};

            JOptionPane.showMessageDialog(null,
                    "<html><div style='font-size:14px;'>Por favor completa los siguientes campos:<br>" +
                            String.join("<br>", errores) + "</div></html>",
                    "Campos incompletos", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void updateImagePreview(JTextField imageUrlField, JLabel previewLabel) {
        String urlText = imageUrlField.getText().trim();
        if (urlText.isEmpty()) {
            previewLabel.setIcon(null);
            previewLabel.setText("Vista Previa");
            return;
        }

        previewLabel.setIcon(null);
        previewLabel.setText("Cargando...");
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL(urlText);
                Image image = ImageIO.read(url);
                if (image == null) throw new Exception("No es una imagen válida");
                return new ImageIcon(image.getScaledInstance(200, 150, Image.SCALE_SMOOTH));
            }

            @Override
            protected void done() {
                try {
                    previewLabel.setText(null);
                    previewLabel.setIcon(get());
                } catch (Exception ex) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("<html><center>URL no válida o<br>imagen no encontrada</center></html>");
                }
            }
        }.execute();
    }
}

// Icono personalizado para el logo
class LogoIcon implements Icon {
    private final int width;
    private final int height;

    public LogoIcon(int size) {
        this.width = size;
        this.height = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(204, 0, 0));
        g2d.fillRect(x, y, width, height);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, height / 2));
        g2d.setColor(Color.WHITE);
        String texto1 = "UD";
        int strWidth1 = g2d.getFontMetrics().stringWidth(texto1);
        g2d.drawString(texto1, x + (width - strWidth1) / 2, y + (height / 2) - 5);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, height / 3));
        String texto2 = "FORO";
        int strWidth2 = g2d.getFontMetrics().stringWidth(texto2);
        g2d.drawString(texto2, x + (width - strWidth2) / 2, y + (height - 10));
        g2d.dispose();
    }

    @Override
    public int getIconWidth() { return width; }
    @Override
    public int getIconHeight() { return height; }
}

// Clase principal
public class MainFusionado {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Color rojoPrincipal = new Color(204, 0, 0);
            Color blancoFondo = new Color(250, 250, 250);
            Color grisTexto = new Color(60, 60, 60);
            Color rojoFocus = new Color(230, 50, 50);

            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}

            UIManager.put("control", blancoFondo);
            UIManager.put("nimbusLightBackground", blancoFondo);
            UIManager.put("Panel.background", blancoFondo);
            UIManager.put("OptionPane.background", blancoFondo);
            UIManager.put("text", grisTexto);
            UIManager.put("OptionPane.messageForeground", grisTexto);
            UIManager.put("Button.background", rojoPrincipal);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("nimbusFocus", rojoFocus);
            UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(rojoPrincipal, 2, true),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 16));
            UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 18));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 16));
            UIManager.put("PasswordField.font", new Font("Segoe UI", Font.PLAIN, 16));
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 16));

            Icon logo = new LogoIcon(64);
            AuthService authService = new AuthService();
            IdeaService ideaService = new IdeaService();
            Foro foro = new Foro(ideaService);
            Usuario usuarioActual = null;
            boolean salir = false;

            while (!salir) {
                String tituloSesion = (usuarioActual != null)
                        ? "<html>Iniciaste sesión como <b>" + usuarioActual.getUsername() + "</b></html>"
                        : "Bienvenido a UD FORO";
                String[] opciones = (usuarioActual == null)
                        ? new String[]{"Registrarse", "Iniciar sesión", "Ver muro de ideas", "Salir"}
                        : usuarioActual.getUsername().equals("admin")
                        ? new String[]{"Ver muro de ideas", "Gestionar ideas", "Cerrar sesión", "Salir"}
                        : new String[]{"Ver muro de ideas", "Enviar idea", "Cerrar sesión", "Salir"};

                int opcion = JOptionPane.showOptionDialog(null, tituloSesion, "Menú Principal",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, logo, opciones, opciones[0]);

                if (usuarioActual == null) {
                    switch (opcion) {
                        case 0: // Registrarse
                            String[] registro = DialogUtils.pedirUsuarioYContrasena("Registro");
                            if (registro != null) {
                                if (authService.registrarUsuario(registro[0], registro[1])) {
                                    JOptionPane.showMessageDialog(null,
                                            "<html><div style='font-size:16px;'>Registro exitoso. Ahora puede iniciar sesión.</div></html>");
                                } else {
                                    JOptionPane.showMessageDialog(null,
                                            "<html><div style='font-size:16px;'>El usuario ya existe.</div></html>");
                                }
                            }
                            break;
                        case 1: // Iniciar sesión
                            String[] login = DialogUtils.pedirUsuarioYContrasena("Inicio de Sesión");
                            if (login != null) {
                                usuarioActual = authService.iniciarSesion(login[0], login[1]);
                                if (usuarioActual != null) {
                                    JOptionPane.showMessageDialog(null,
                                            "<html><div style='font-size:16px;'>Inicio de sesión exitoso. ¡Bienvenido, " + login[0] + "!</div></html>");
                                } else {
                                    JOptionPane.showMessageDialog(null,
                                            "<html><div style='font-size:16px;'>Usuario o contraseña incorrectos.</div></html>");
                                }
                            }
                            break;
                        case 2: // Ver muro de ideas
                            foro.mostrarMuroGlobalIdeas(usuarioActual);
                            break;
                        case 3: // Salir
                        case JOptionPane.CLOSED_OPTION:
                            salir = true;
                            break;
                    }
                } else if (usuarioActual.getUsername().equals("admin")) {
                    switch (opcion) {
                        case 0: // Ver muro de ideas
                            foro.mostrarMuroGlobalIdeas(usuarioActual);
                            break;
                        case 1: // Gestionar ideas
                            foro.mostrarIdeasPendientes();
                            break;
                        case 2: // Cerrar sesión
                            usuarioActual = null;
                            JOptionPane.showMessageDialog(null,
                                    "<html><div style='font-size:16px;'>Sesión cerrada.</div></html>");
                            break;
                        case 3: // Salir
                        case JOptionPane.CLOSED_OPTION:
                            salir = true;
                            break;
                    }
                } else {
                    switch (opcion) {
                        case 0: // Ver muro de ideas
                            foro.mostrarMuroGlobalIdeas(usuarioActual);
                            break;
                        case 1: // Enviar idea
                            String[] datosIdea = IdeaInputDialog.mostrarDialogo();
                            if (datosIdea != null) {
                                foro.agregarIdea(datosIdea[0], datosIdea[1], datosIdea[2], datosIdea[3], datosIdea[4]);
                            }
                            break;
                        case 2: // Cerrar sesión
                            usuarioActual = null;
                            JOptionPane.showMessageDialog(null,
                                    "<html><div style='font-size:16px;'>Sesión cerrada.</div></html>");
                            break;
                        case 3: // Salir
                        case JOptionPane.CLOSED_OPTION:
                            salir = true;
                            break;
                    }
                }
            }
        });
    }
}