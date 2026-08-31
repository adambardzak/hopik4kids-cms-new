package cz.hopik4kids.cms.records.service;

import cz.hopik4kids.cms.kernel.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Private file storage for accounting/HR documents (prd todo #8). Files live outside the public
 * media dir and are streamed only via an authenticated endpoint. Accepts PDFs and common images.
 */
@Service
public class RecordStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp", "image/heic");

    private static final long MAX_BYTES = 20L * 1024 * 1024; // 20 MB

    private final Path storageDir;

    public RecordStorageService(@Value("${app.records.storage-dir:./records-storage}") String storageDir) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    public record Stored(String storedName, String contentType, long size) {}

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Soubor je prázdný");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Soubor je příliš velký (max 20 MB)");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct)) {
            throw ApiException.badRequest("UNSUPPORTED_TYPE", "Podporované jsou PDF a obrázky");
        }
        String storedName = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(storedName).normalize();
            if (!target.startsWith(storageDir)) {
                throw ApiException.badRequest("INVALID_PATH", "Neplatná cesta");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Nepodařilo se uložit soubor");
        }
        return new Stored(storedName, ct, file.getSize());
    }

    public byte[] read(String storedName) {
        try {
            Path target = storageDir.resolve(storedName).normalize();
            if (!target.startsWith(storageDir) || !Files.exists(target)) {
                throw ApiException.notFound("Soubor nenalezen");
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Nepodařilo se načíst soubor");
        }
    }

    public void delete(String storedName) {
        try {
            Path target = storageDir.resolve(storedName).normalize();
            if (target.startsWith(storageDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // best-effort; the DB row is the source of truth
        }
    }

    private static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot).toLowerCase() : "";
    }
}
