package main.multiuser;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Sistema de bloqueos para sincronización multiusuario.
 * Gestiona bloqueos de archivos para evitar conflictos de escritura concurrente.
 */

public class LockManager {
    private static final String SHARED_PATH = "\\\\LABING501-06\\Users\\estudiantes\\Documents\\Compartida";
    private static final int MAX_LOCK_WAIT_SECONDS = 10;
    private static final int LOCK_RETRY_DELAY_MS = 100;

    /**
     * Intenta obtener un bloqueo para un archivo específico.
     * 
     * @param fileName Nombre del archivo a bloquear (sin extensión)
     * @return true si se obtuvo el bloqueo, false si no se pudo
     */
    public static boolean acquireLock(String fileName) {
        String lockFileName = fileName + ".lock";
        Path lockPath = Paths.get(SHARED_PATH, lockFileName);
        
        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(MAX_LOCK_WAIT_SECONDS);
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                // Intenta crear el archivo de bloqueo
                Files.createFile(lockPath);
                
                // Escribe información del proceso que obtuvo el bloqueo
                String lockInfo = System.getProperty("user.name") + "@" + 
                                System.getProperty("computer.name", "unknown") + 
                                " - " + System.currentTimeMillis();
                Files.write(lockPath, lockInfo.getBytes());
                
                return true;
            } catch (FileAlreadyExistsException e) {
                // El archivo ya existe, alguien más tiene el bloqueo
                try {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (IOException e) {
                System.err.println("Error al crear bloqueo: " + e.getMessage());
                return false;
            }
        }
        
        return false; // Timeout
    }

    /**
     * Libera el bloqueo de un archivo específico.
     * 
     * @param fileName Nombre del archivo a desbloquear (sin extensión)
     */
    public static void releaseLock(String fileName) {
        String lockFileName = fileName + ".lock";
        Path lockPath = Paths.get(SHARED_PATH, lockFileName);
        
        try {
            Files.deleteIfExists(lockPath);
        } catch (IOException e) {
            System.err.println("Error al liberar bloqueo: " + e.getMessage());
        }
    }

    /**
     * Verifica si un archivo está bloqueado.
     * 
     * @param fileName Nombre del archivo a verificar
     * @return true si está bloqueado, false si no
     */
    public static boolean isLocked(String fileName) {
        String lockFileName = fileName + ".lock";
        Path lockPath = Paths.get(SHARED_PATH, lockFileName);
        return Files.exists(lockPath);
    }

    /**
     * Ejecuta una operación con bloqueo automático.
     * 
     * @param fileName Nombre del archivo a bloquear
     * @param operation Operación a ejecutar
     * @return true si la operación se ejecutó correctamente
     */
    public static boolean executeWithLock(String fileName, Runnable operation) {
        if (acquireLock(fileName)) {
            try {
                operation.run();
                return true;
            } finally {
                releaseLock(fileName);
            }
        }
        return false;
    }

    /**
     * Obtiene la ruta completa del directorio compartido.
     */
    public static String getSharedPath() {
        return SHARED_PATH;
    }

    /**
     * Verifica si el directorio compartido está disponible.
     */
    public static boolean isSharedPathAvailable() {
        return Files.exists(Paths.get(SHARED_PATH));
    }
}
