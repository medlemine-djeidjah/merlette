package com.merlette.backend.observation;

import com.merlette.backend.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Component
public class PhotoStorage {
    private final Path directory;
    public PhotoStorage(@Value("${merlette.upload-directory}") String directory) throws IOException {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }
    public Path directory() { return directory; }
    public List<String> save(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > 5) {
            throw ApiException.invalid("Ajoute entre 1 et 5 photos.");
        }
        var written = new ArrayList<Path>();
        // File writes are compensated when the database transaction rolls back (including commit failure).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) written.forEach(PhotoStorage.this::delete);
            }
        });
        var urls = new ArrayList<String>();
        for (var file : files) {
            if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw ApiException.invalid("Photo vide ou supérieure à 5 Mo.");
            try {
                byte[] bytes = file.getBytes();
                String extension = extension(bytes);
                Path target = directory.resolve(UUID.randomUUID() + extension);
                written.add(target);
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
                urls.add("/uploads/" + target.getFileName());
            } catch (IOException e) { throw new IllegalStateException("Impossible d'enregistrer la photo.", e); }
        }
        return urls;
    }
    static String extension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return ".jpg";
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) return ".png";
        if (bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
            && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return ".webp";
        throw ApiException.invalid("Seules les images JPEG, PNG et WEBP sont acceptées.");
    }
    private void delete(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).error("Nettoyage impossible : {}", path, e); }
    }
}
