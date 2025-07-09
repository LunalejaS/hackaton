package foro;

import main.multiuser.LockManager;
import main.multiuser.SyncService;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servicio multiusuario para gestionar la persistencia y operaciones sobre las ideas del foro.
 * Utiliza bloqueos y archivos .properties para sincronización entre múltiples usuarios.
 */
public class MultiUserIdeaService {
    private static final Logger logger = Logger.getLogger(MultiUserIdeaService.class.getName());
    private final List<Idea> ideas = new ArrayList<>();
    private static final String ARCHIVO_IDEAS = "ideas.properties";
    private final SyncService syncService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean isShuttingDown = false;

    public MultiUserIdeaService() {
        syncService = SyncService.getInstance();

        // Configurar listener para cambios en ideas
        syncService.setOnIdeasChanged(() -> {
            if (!isShuttingDown) {
                lock.writeLock().lock();
                try {
                    cargarIdeasDesdeArchivo();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });

        // Cargar ideas iniciales y iniciar sincronización
        cargarIdeasDesdeArchivo();
        syncService.start();
    }

    public List<Idea> getIdeas() {
        syncService.forceUpdate();
        lock.readLock().lock();
        try {
            return new ArrayList<>(ideas);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Idea> getIdeasPendientes() {
        syncService.forceUpdate();
        lock.readLock().lock();
        try {
            return ideas.stream()
                    .filter(idea -> idea.getEstado() == Idea.Estado.PENDIENTE)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Idea> getIdeasAprobadas() {
        syncService.forceUpdate();
        lock.readLock().lock();
        try {
            return ideas.stream()
                    .filter(idea -> idea.getEstado() == Idea.Estado.APROBADA)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void agregarIdea(Idea idea) {
        if (idea == null) {
            throw new IllegalArgumentException("La idea no puede ser null");
        }
        
        boolean success = LockManager.executeWithLock("ideas", () -> {
            lock.writeLock().lock();
            try {
                cargarIdeasDesdeArchivo();
                ideas.add(idea);
                guardarIdeasAArchivo();
            } finally {
                lock.writeLock().unlock();
            }
        });

        if (!success) {
            throw new RuntimeException("No se pudo obtener el bloqueo para agregar la idea");
        }
    }

    public void aprobarIdea(Idea idea) {
        if (idea == null) {
            throw new IllegalArgumentException("La idea no puede ser null");
        }
        
        boolean success = LockManager.executeWithLock("ideas", () -> {
            lock.writeLock().lock();
            try {
                cargarIdeasDesdeArchivo();
                boolean encontrada = false;
                for (Idea i : ideas) {
                    if (sonIguales(i, idea)) {
                        i.aprobar();
                        encontrada = true;
                        break;
                    }
                }
                if (!encontrada) {
                    logger.warning("No se encontró la idea para aprobar: " + idea.getTitulo());
                }
                guardarIdeasAArchivo();
            } finally {
                lock.writeLock().unlock();
            }
        });

        if (!success) {
            throw new RuntimeException("No se pudo obtener el bloqueo para aprobar la idea");
        }
    }

    public void desaprobarIdea(Idea idea) {
        if (idea == null) {
            throw new IllegalArgumentException("La idea no puede ser null");
        }
        
        boolean success = LockManager.executeWithLock("ideas", () -> {
            lock.writeLock().lock();
            try {
                cargarIdeasDesdeArchivo();
                boolean encontrada = false;
                for (Idea i : ideas) {
                    if (sonIguales(i, idea)) {
                        i.desaprobar();
                        encontrada = true;
                        break;
                    }
                }
                if (!encontrada) {
                    logger.warning("No se encontró la idea para desaprobar: " + idea.getTitulo());
                }
                guardarIdeasAArchivo();
            } finally {
                lock.writeLock().unlock();
            }
        });

        if (!success) {
            throw new RuntimeException("No se pudo obtener el bloqueo para desaprobar la idea");
        }
    }

    public void eliminarIdea(Idea idea) {
        if (idea == null) {
            throw new IllegalArgumentException("La idea no puede ser null");
        }
        
        boolean success = LockManager.executeWithLock("ideas", () -> {
            lock.writeLock().lock();
            try {
                cargarIdeasDesdeArchivo();
                boolean eliminada = ideas.removeIf(i -> sonIguales(i, idea));
                if (!eliminada) {
                    logger.warning("No se encontró la idea para eliminar: " + idea.getTitulo());
                }
                guardarIdeasAArchivo();
            } finally {
                lock.writeLock().unlock();
            }
        });

        if (!success) {
            throw new RuntimeException("No se pudo obtener el bloqueo para eliminar la idea");
        }
    }

    public void guardarIdeas() {
        boolean success = LockManager.executeWithLock("ideas", () -> {
            lock.readLock().lock();
            try {
                guardarIdeasAArchivo();
            } finally {
                lock.readLock().unlock();
            }
        });

        if (!success) {
            throw new RuntimeException("No se pudo obtener el bloqueo para guardar las ideas");
        }
    }

    private boolean sonIguales(Idea i1, Idea i2) {
        return Objects.equals(i1.getCodigoEstudiante(), i2.getCodigoEstudiante()) &&
               Objects.equals(i1.getTitulo(), i2.getTitulo());
    }

    private void guardarIdeasAArchivo() {
        Path filePath = Paths.get(LockManager.getSharedPath(), ARCHIVO_IDEAS);
        Properties props = new Properties();

        for (int i = 0; i < ideas.size(); i++) {
            Idea idea = ideas.get(i);
            String prefix = "idea." + i + ".";
            
            props.setProperty(prefix + "titulo", idea.getTitulo());
            props.setProperty(prefix + "descripcion", idea.getDescripcion());
            props.setProperty(prefix + "autor", idea.getCodigoEstudiante());
            props.setProperty(prefix + "estado", idea.getEstado().name());
        }

        try (OutputStream out = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Ideas del sistema - Guardado: " + java.time.LocalDateTime.now());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error guardando ideas", e);
            guardarErrorEnTxt("Error guardando ideas: " + e.getMessage());
        }
    }

    private void cargarIdeasDesdeArchivo() {
        Path filePath = Paths.get(LockManager.getSharedPath(), ARCHIVO_IDEAS);
        
        if (!Files.exists(filePath)) {
            logger.info("Archivo de ideas no existe, se creará uno nuevo");
            ideas.clear();
            return;
        }

        Properties props = new Properties();
        List<Idea> nuevasIdeas = new ArrayList<>();

        try (InputStream in = Files.newInputStream(filePath)) {
            props.load(in);

            int i = 0;
            while (true) {
                String titulo = props.getProperty("idea." + i + ".titulo");
                if (titulo == null || titulo.trim().isEmpty()) break;

                String descripcion = props.getProperty("idea." + i + ".descripcion", "");
                String autor = props.getProperty("idea." + i + ".autor", "desconocido");
                String estadoStr = props.getProperty("idea." + i + ".estado", "PENDIENTE");

                try {
                    Idea idea = new Idea(titulo, descripcion, autor, estadoStr, estadoStr);
                    
                    // Configurar estado con manejo de errores
                    switch (estadoStr.toUpperCase()) {
                        case "APROBADA":
                            idea.aprobar();
                            break;
                        case "RECHAZADA":
                            idea.desaprobar();
                            break;
                        case "PENDIENTE":
                            // Estado por defecto
                            break;
                        default:
                            logger.warning("Estado desconocido para idea " + titulo + ": " + estadoStr);
                            break;
                    }

                    nuevasIdeas.add(idea);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error cargando idea " + i + ": " + titulo, e);
                }
                
                i++;
            }

            ideas.clear();
            ideas.addAll(nuevasIdeas);
            logger.info("Cargadas " + ideas.size() + " ideas desde archivo");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error cargando ideas desde archivo", e);
            guardarErrorEnTxt("Error cargando ideas: " + e.getMessage());
        }
    }

    private void guardarErrorEnTxt(String mensaje) {
        try (FileWriter fw = new FileWriter("errores_multiuser.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(java.time.LocalDateTime.now() + " - " + mensaje);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error registrando error en archivo", ex);
        }
    }

    public void shutdown() {
        isShuttingDown = true;
        lock.writeLock().lock();
        try {
            syncService.stop();
        } finally {
            lock.writeLock().unlock();
        }
    }
}