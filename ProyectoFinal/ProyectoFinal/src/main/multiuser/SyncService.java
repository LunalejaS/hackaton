package main.multiuser;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de sincronización que monitorea cambios en archivos compartidos
 * y notifica a los componentes cuando hay actualizaciones.
 */

public class SyncService {
    private static SyncService instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    // Listeners para notificar cambios
    private Runnable onIdeasChanged;
    private Runnable onUsersChanged;
    
    // Timestamps para detectar cambios
    private long lastIdeasModified = 0;
    private long lastUsersModified = 0;
    
    private SyncService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public static synchronized SyncService getInstance() {
        if (instance == null) {
            instance = new SyncService();
        }
        return instance;
    }
    
    /**
     * Inicia el servicio de sincronización.
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            // Revisa cambios cada 2 segundos
            scheduler.scheduleAtFixedRate(this::checkForChanges, 0, 2, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Detiene el servicio de sincronización.
     */
    public void stop() {
        if (isRunning) {
            isRunning = false;
            scheduler.shutdown();
        }
    }
    
    /**
     * Establece el listener para cambios en ideas.
     */
    public void setOnIdeasChanged(Runnable listener) {
        this.onIdeasChanged = listener;
    }
    
    /**
     * Establece el listener para cambios en usuarios.
     */
    public void setOnUsersChanged(Runnable listener) {
        this.onUsersChanged = listener;
    }
    
    /**
     * Verifica si hay cambios en los archivos compartidos.
     */
    private void checkForChanges() {
        try {
            // Verificar cambios en ideas
            Path ideasPath = Paths.get(LockManager.getSharedPath(), "ideas.dat");
            if (Files.exists(ideasPath)) {
                long currentIdeasModified = Files.getLastModifiedTime(ideasPath).toMillis();
                if (currentIdeasModified > lastIdeasModified) {
                    lastIdeasModified = currentIdeasModified;
                    if (onIdeasChanged != null) {
                        onIdeasChanged.run();
                    }
                }
            }
            
            // Verificar cambios en usuarios
            Path usersPath = Paths.get(LockManager.getSharedPath(), "usuarios.dat");
            if (Files.exists(usersPath)) {
                long currentUsersModified = Files.getLastModifiedTime(usersPath).toMillis();
                if (currentUsersModified > lastUsersModified) {
                    lastUsersModified = currentUsersModified;
                    if (onUsersChanged != null) {
                        onUsersChanged.run();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error verificando cambios: " + e.getMessage());
        }
    }
    
    /**
     * Fuerza una actualización inmediata.
     */
    public void forceUpdate() {
        checkForChanges();
    }
}
