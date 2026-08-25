package cz.hopik4kids.cms.core.service;

import cz.hopik4kids.cms.core.domain.Media;
import cz.hopik4kids.cms.core.repository.MediaRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Media upload to local disk (prd §5.6, §11 - local disk or S3). Stores the file under
 * {@code app.media.storage-dir} and persists a {@link Media} row whose url points at the
 * public media base URL.
 */
@Service
public class MediaService {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final MediaRepository media;
    private final AuditService audit;
    private final Path storageDir;
    private final String publicBaseUrl;

    public MediaService(MediaRepository media,
                        AuditService audit,
                        @Value("${app.media.storage-dir}") String storageDir,
                        @Value("${app.media.public-base-url}") String publicBaseUrl) {
        this.media = media;
        this.audit = audit;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public Media upload(MultipartFile file, String alt) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Soubor je prázdný");
        }
        if (!ALLOWED.contains(file.getContentType())) {
            throw ApiException.badRequest("UNSUPPORTED_TYPE", "Nepodporovaný typ souboru");
        }

        String ext = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(filename).normalize();
            if (!target.startsWith(storageDir)) {
                throw ApiException.badRequest("INVALID_PATH", "Neplatná cesta");
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "STORAGE_ERROR", "Nepodařilo se uložit soubor");
        }

        Media m = new Media();
        m.setUrl(publicBaseUrl + "/" + filename);
        m.setAlt(alt);
        m = media.save(m);
        audit.record("upload", "Media", m.getId());
        return m;
    }

    private static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot).toLowerCase() : "";
    }
}
