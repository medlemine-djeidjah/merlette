package com.merlette.backend.service;

import com.merlette.backend.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Enregistre les images sur le disque (dossier uploads/) et les sert sur /uploads/<fichier>. */
@Service
public class FileStorageService {

    public static final int MAX_PHOTOS = 5;
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final String URL_PREFIX = "/uploads/";

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(this.uploadDir);
    }

    /** Vérifie puis enregistre les images ; renvoie leurs URLs publiques. */
    public List<String> saveAll(List<MultipartFile> files) {
        List<String> extensions = new ArrayList<>();
        for (MultipartFile file : files) {
            extensions.add(checkImage(file));
        }
        List<String> urls = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                String name = UUID.randomUUID() + "." + extensions.get(i);
                files.get(i).transferTo(uploadDir.resolve(name));
                urls.add(URL_PREFIX + name);
            }
        } catch (IOException e) {
            deleteAll(urls);
            throw new IllegalStateException("Impossible d'enregistrer l'image", e);
        }
        return urls;
    }

    public void deleteAll(List<String> urls) {
        urls.forEach(this::delete);
    }

    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path file = uploadDir.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(uploadDir)) {
            return; // sécurité : on ne sort jamais du dossier uploads
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // un fichier orphelin n'est pas bloquant
        }
    }

    /** Taille ≤ 5 Mo et contenu réellement JPEG, PNG ou WEBP. Renvoie l'extension. */
    private String checkImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Fichier vide.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "Image trop volumineuse (5 Mo max).");
        }
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            head = Arrays.copyOf(head, read);
        } catch (IOException e) {
            throw ApiException.badRequest("Fichier illisible.");
        }
        String extension = detectImageType(head);
        if (extension == null) {
            throw ApiException.badRequest("Seules les images JPEG, PNG ou WEBP sont acceptées.");
        }
        return extension;
    }

    /** Reconnaît le format grâce aux premiers octets du fichier. */
    static String detectImageType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
